package com.ironcoders.aquaconectabackend.predictive.application.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction.PredictionStatus;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.GeneratePredictionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.services.ConsumptionCalculationService;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionCommandService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.MLServiceClient;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.DailyPredictionDTO;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionRequest;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionResponse;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.ConsumptionPredictionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.MonitoringContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.SubscriptionContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto.EventDTO;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for prediction command operations.
 * Handles generation and persistence of consumption predictions.
 */
@Service
public class PredictionCommandServiceImpl implements PredictionCommandService {

    private static final Logger log = LoggerFactory.getLogger(PredictionCommandServiceImpl.class);

    private final ConsumptionCalculationService consumptionCalculationService;
    private final ConsumptionPredictionRepository predictionRepository;
    private final SubscriptionContextFacade subscriptionContextFacade;
    private final MonitoringContextFacade monitoringContextFacade;  // ← AGREGAR
    private final MLServiceClient mlServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for dependency injection.
     */
    public PredictionCommandServiceImpl(
            ConsumptionCalculationService consumptionCalculationService,
            ConsumptionPredictionRepository predictionRepository,
            SubscriptionContextFacade subscriptionContextFacade,
            MonitoringContextFacade monitoringContextFacade,  // ← AGREGAR
            MLServiceClient mlServiceClient,
            ObjectMapper objectMapper) {
        this.consumptionCalculationService = consumptionCalculationService;
        this.predictionRepository = predictionRepository;
        this.subscriptionContextFacade = subscriptionContextFacade;
        this.monitoringContextFacade = monitoringContextFacade;  // ← AGREGAR
        this.mlServiceClient = mlServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Optional<ConsumptionPrediction> handle(GeneratePredictionCommand command) {
        try {
            log.info("Starting prediction generation for resident: {}", command.residentId());

            command.validate();

            // 1. Calculate daily consumption for the last 30 days
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            var calculateCommand = new CalculateDailyConsumptionCommand(
                command.residentId(),
                startDate,
                endDate
            );
            
            List<WaterConsumption> consumptions = 
                consumptionCalculationService.handle(calculateCommand);

            if (consumptions.size() < 7) {
                log.warn("Insufficient consumption data for resident: {}. Need at least 7 days, got: {}", 
                    command.residentId(), consumptions.size());
                return Optional.empty();
            }

            log.info("Found {} days of consumption data for resident: {}", 
                consumptions.size(), command.residentId());

            // 2. Get water tank size
            Double waterTankSize = subscriptionContextFacade
                .getWaterTankSizeByResidentId(command.residentId())
                .orElse(1000.0);

            // 3. ✅ NUEVO: Get current water level from latest event
            Double currentWaterLevel = getCurrentWaterLevel(command.residentId(), waterTankSize);
            
            log.info("Current water level for resident {}: {} L ({}% of {} L tank)", 
                command.residentId(), currentWaterLevel, 
                (currentWaterLevel / waterTankSize) * 100, waterTankSize);

            // 4. Prepare ML request
            MLPredictionRequest mlRequest = buildMLRequest(
                command.residentId(), 
                consumptions, 
                currentWaterLevel  // ← Usar nivel actual, no tamaño del tanque
            );

            // 5. Call ML service
            log.info("Calling ML service for resident: {}", command.residentId());
            MLPredictionResponse mlResponse = mlServiceClient.getPrediction(mlRequest);

            // ✅ NUEVO: Ajustar predicciones considerando el agua disponible
            MLPredictionResponse adjustedResponse = adjustPredictionsForWaterAvailability(
                mlResponse, 
                currentWaterLevel
            );

            // 6. Mark old predictions as OUTDATED
            markOldPredictionsAsOutdated(command.residentId());

            // 7. Create and save new prediction
            ConsumptionPrediction prediction = createPrediction(
                command.residentId(), 
                adjustedResponse
            );
            
            predictionRepository.save(prediction);

            log.info("Prediction generated successfully for resident: {} with confidence: {}", 
                command.residentId(), prediction.getConfidenceScore());

            return Optional.of(prediction);

        } catch (Exception e) {
            log.error("Error generating prediction for resident: {}", command.residentId(), e);
            return Optional.empty();
        }
    }

    private MLPredictionResponse adjustPredictionsForWaterAvailability(MLPredictionResponse mlResponse,
            Double currentWaterLevel) {
    
    List<DailyPredictionDTO> originalPredictions = mlResponse.getNext7DaysPredictions();
    List<DailyPredictionDTO> adjustedPredictions = new ArrayList<>();
    
    Double remainingWater = currentWaterLevel;
    LocalDate runoutDate = null;
    int daysUntilRunout = 0;
    boolean waterDepleted = false;
    
    log.info("Adjusting predictions for water availability. Current level: {} L", currentWaterLevel);
    
    for (int i = 0; i < originalPredictions.size(); i++) {
        DailyPredictionDTO originalPrediction = originalPredictions.get(i);
        Double predictedConsumption = originalPrediction.getPredictedConsumption();
        
        if (waterDepleted) {
            // Water already depleted, set consumption to 0 for remaining days
            adjustedPredictions.add(new DailyPredictionDTO(
                originalPrediction.getDate(),
                0.0,  // No consumption possible
                originalPrediction.getDayOfWeek()
            ));
            log.debug("Day {}: Water depleted, setting consumption to 0", i + 1);
            continue;
        }
        
        if (predictedConsumption <= remainingWater) {
            // Normal case: enough water for predicted consumption
            adjustedPredictions.add(originalPrediction);
            remainingWater -= predictedConsumption;
            daysUntilRunout = i + 1;
            
            log.debug("Day {}: Predicted {} L, Remaining {} L", 
                i + 1, predictedConsumption, remainingWater);
            
        } else {
            // Critical case: predicted consumption exceeds available water
            // Water will run out during this day
            adjustedPredictions.add(new DailyPredictionDTO(
                originalPrediction.getDate(),
                remainingWater,  // Can only consume what's left
                originalPrediction.getDayOfWeek()
            ));
            
            log.warn("Day {}: Predicted {} L but only {} L available. Water will run out!", 
                i + 1, predictedConsumption, remainingWater);
            
            runoutDate = LocalDate.parse(originalPrediction.getDate());
            daysUntilRunout = i + 1;
            remainingWater = 0.0;
            waterDepleted = true;
        }
    }
    
    // If water never depletes in 7 days, estimate runout date
    if (runoutDate == null && remainingWater > 0) {
        Double avgConsumption = mlResponse.getDailyAverageConsumption();
        if (avgConsumption > 0) {
            int additionalDays = (int) Math.ceil(remainingWater / avgConsumption);
            daysUntilRunout = 7 + additionalDays;
            runoutDate = LocalDate.now().plusDays(daysUntilRunout);
        } else {
            // No consumption, water never runs out
            daysUntilRunout = 999;
            runoutDate = LocalDate.now().plusYears(1);
        }
    }
    
    // Calculate total predicted consumption (sum of adjusted predictions)
    Double totalPredicted = adjustedPredictions.stream()
        .mapToDouble(DailyPredictionDTO::getPredictedConsumption)
        .sum();
    
    log.info("Adjusted predictions: {} days until runout, total consumption: {} L", 
        daysUntilRunout, totalPredicted);
    
    // Create adjusted response
    return new MLPredictionResponse(
        mlResponse.getUserId(),
        mlResponse.getDailyAverageConsumption(),
        adjustedPredictions,
        runoutDate.toString(),
        daysUntilRunout,
        mlResponse.getConfidenceScore(),
        currentWaterLevel,
        totalPredicted
    );
}

    /**
     * ✅ NUEVO MÉTODO: Gets the current water level from the most recent event.
     * 
     * @param residentId The resident ID
     * @param waterTankSize The tank size to convert percentage to liters
     * @return Current water level in liters
     */
    private Double getCurrentWaterLevel(Long residentId, Double waterTankSize) {
        try {
            // Get most recent events for this resident (last 2 days to be safe)
            List<EventDTO> recentEvents = monitoringContextFacade.getEventsByResidentId(
                residentId,
                LocalDate.now().minusDays(2),
                LocalDate.now()
            );

            if (recentEvents.isEmpty()) {
                log.warn("No recent events found for resident: {}. Using default water level (50% of tank).", 
                    residentId);
                return waterTankSize * 0.5; // Default: 50% of tank
            }

            // Get the most recent event
            EventDTO latestEvent = recentEvents.stream()
                .max(Comparator.comparing(EventDTO::getTimestamp))
                .orElseThrow();

            // Parse level as percentage and convert to liters
            String levelValue = latestEvent.getLevelValue();
            Double percentage = parsePercentage(levelValue);
            Double currentLiters = (percentage / 100.0) * waterTankSize;

            log.debug("Latest event for resident {} at {}: {}% = {} L", 
                residentId, latestEvent.getTimestamp(), percentage, currentLiters);

            return currentLiters;

        } catch (Exception e) {
            log.error("Error getting current water level for resident: {}", residentId, e);
            return waterTankSize * 0.5; // Fallback: 50% of tank
        }
    }

    /**
     * ✅ NUEVO MÉTODO: Helper method to parse percentage value.
     * Handles both integer format (85) and decimal format (0.85).
     */
    private Double parsePercentage(String levelValue) {
        try {
            // Remove any non-numeric characters except decimal point
            String cleaned = levelValue.replaceAll("[^0-9.]", "");
            Double value = Double.parseDouble(cleaned);
            
            // Detect format:
            // If value is between 0 and 1, it's decimal format (0.85 = 85%)
            // If value is between 1 and 100, it's percentage format (85 = 85%)
            Double percentage;
            if (value <= 1.0) {
                // Decimal format: convert to percentage
                percentage = value * 100.0;
                log.debug("Converted decimal {} to percentage {}", value, percentage);
            } else {
                // Already in percentage format
                percentage = value;
            }
            
            // Validate range (0-100)
            if (percentage < 0 || percentage > 100) {
                log.warn("Percentage out of range (0-100): {}. Clamping.", percentage);
                percentage = Math.max(0, Math.min(100, percentage));
            }
            
            return percentage;
            
        } catch (NumberFormatException e) {
            log.warn("Failed to parse percentage value: {}", levelValue);
            return 0.0;
        }
    }

    /**
     * Builds the ML service request from consumption data
     */
    private MLPredictionRequest buildMLRequest(
            Long residentId, 
            List<WaterConsumption> consumptions,
            Double currentWaterLevel) {
        
        var historicalData = consumptions.stream()
            .map(wc -> new MLPredictionRequest.HistoricalDataPoint(
                wc.getDate().toString(),
                wc.getConsumption()
            ))
            .toList();

        return new MLPredictionRequest(
            residentId.toString(),
            historicalData,
            currentWaterLevel  // ← Ahora usa el nivel actual correcto
        );
    }

    /**
     * Creates a ConsumptionPrediction entity from ML response
     */
    private ConsumptionPrediction createPrediction(
            Long residentId, 
            MLPredictionResponse mlResponse) {
        
        try {
            String predictionsJson = objectMapper.writeValueAsString(
                mlResponse.getNext7DaysPredictions()
            );

            LocalDate runoutDate = LocalDate.parse(mlResponse.getWaterRunoutDate());

            return new ConsumptionPrediction(
                residentId,
                mlResponse.getDailyAverageConsumption(),
                predictionsJson,
                runoutDate,
                mlResponse.getDaysUntilRunout(),
                mlResponse.getConfidenceScore(),
                mlResponse.getCurrentWaterLevel(),
                mlResponse.getTotalPredictedConsumption7Days()
            );

        } catch (JsonProcessingException e) {
            log.error("Error creating prediction entity", e);
            throw new RuntimeException("Failed to serialize predictions", e);
        }
    }

    /**
     * Marks all active predictions for a resident as OUTDATED
     */
    private void markOldPredictionsAsOutdated(Long residentId) {
        List<ConsumptionPrediction> activePredictions = 
            predictionRepository.findByResidentIdAndStatus(residentId, PredictionStatus.ACTIVE);

        activePredictions.forEach(ConsumptionPrediction::markAsOutdated);
        
        if (!activePredictions.isEmpty()) {
            predictionRepository.saveAll(activePredictions);
            log.info("Marked {} old predictions as OUTDATED for resident: {}", 
                activePredictions.size(), residentId);
        }
    }
}