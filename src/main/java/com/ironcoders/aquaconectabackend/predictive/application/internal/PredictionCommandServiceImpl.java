package com.ironcoders.aquaconectabackend.predictive.application.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            log.info("Starting prediction generation for subscription: {} (resident: {})", 
                command.subscriptionId(), command.residentId());

            command.validate();

            // 1. Validate subscription belongs to resident
            if (!subscriptionContextFacade.isSubscriptionOwnedByResident(
                    command.subscriptionId(), command.residentId())) {
                log.error("Subscription {} does not belong to resident {}", 
                    command.subscriptionId(), command.residentId());
                return Optional.empty();
            }

            // 2. Get sensor and device info
            Optional<Long> sensorIdOpt = subscriptionContextFacade
                .getSensorIdBySubscription(command.subscriptionId());
            
            if (sensorIdOpt.isEmpty()) {
                log.error("No sensor found for subscription: {}", command.subscriptionId());
                return Optional.empty();
            }
            
            Long sensorId = sensorIdOpt.get();

            // 3. Calculate daily consumption for the last 30 days
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            var calculateCommand = new CalculateDailyConsumptionCommand(
                command.subscriptionId(),  // PRIMARY identifier
                command.residentId(),      // SECONDARY identifier
                startDate,
                endDate
            );
            
            List<WaterConsumption> consumptions = 
                consumptionCalculationService.handle(calculateCommand);

            if (consumptions.size() < 7) {
                log.warn("Insufficient consumption data for subscription: {}. Need at least 7 days, got: {}", 
                    command.subscriptionId(), consumptions.size());
                return Optional.empty();
            }

            log.info("Found {} days of consumption data for subscription: {}", 
                consumptions.size(), command.subscriptionId());

            // 4. Get water tank size
            Double waterTankSize = subscriptionContextFacade
                .getWaterTankSizeBySubscription(command.subscriptionId())
                .orElse(1000.0);

            // 5. ✅ Get current water level from latest event
            Double currentWaterLevel = getCurrentWaterLevel(
                command.subscriptionId(), sensorId, waterTankSize);
            
            log.info("Current water level for subscription {}: {} L ({}% of {} L tank)", 
                command.subscriptionId(), currentWaterLevel, 
                (currentWaterLevel / waterTankSize) * 100, waterTankSize);

            // 6. Prepare ML request
            MLPredictionRequest mlRequest = buildMLRequest(
                command.subscriptionId(), 
                consumptions, 
                currentWaterLevel
            );

            // 7. Call ML service
            log.info("Calling ML service for subscription: {}", command.subscriptionId());
            MLPredictionResponse mlResponse = mlServiceClient.getPrediction(mlRequest);

            // 8. Adjust predictions for water availability and correct dates
            MLPredictionResponse adjustedResponse = adjustPredictionsForWaterAvailability(
                mlResponse, 
                currentWaterLevel,
                consumptions
            );

            // 9. Mark old predictions as OUTDATED
            markOldPredictionsAsOutdated(command.subscriptionId());

            // 10. Create and save new prediction
            ConsumptionPrediction prediction = createPrediction(
                command.subscriptionId(),
                command.residentId(),
                sensorId,
                adjustedResponse
            );
            
            predictionRepository.save(prediction);

            log.info("Prediction generated successfully for subscription: {} with confidence: {}", 
                command.subscriptionId(), prediction.getConfidenceScore());

            return Optional.of(prediction);

        } catch (Exception e) {
            log.error("Error generating prediction for subscription: {}", command.subscriptionId(), e);
            return Optional.empty();
        }
    }

    private MLPredictionResponse adjustPredictionsForWaterAvailability(
            MLPredictionResponse mlResponse,
            Double currentWaterLevel,
            List<WaterConsumption> consumptions) {
    
    List<DailyPredictionDTO> originalPredictions = mlResponse.getNext7DaysPredictions();
    List<DailyPredictionDTO> adjustedPredictions = new ArrayList<>();
    
    // ✅ NUEVO: Obtener la última fecha de consumo para calcular las fechas correctamente
    LocalDate lastConsumptionDate = consumptions.stream()
        .map(WaterConsumption::getDate)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());
    
    log.info("Last consumption date: {}, starting predictions from: {}", 
        lastConsumptionDate, lastConsumptionDate.plusDays(1));
    
    Double remainingWater = currentWaterLevel;
    LocalDate runoutDate = null;
    int daysUntilRunout = 0;
    boolean waterDepleted = false;
    
    log.info("Adjusting predictions for water availability. Current level: {} L", currentWaterLevel);
    
    for (int i = 0; i < originalPredictions.size(); i++) {
        DailyPredictionDTO originalPrediction = originalPredictions.get(i);
        Double predictedConsumption = originalPrediction.getPredictedConsumption();
        
        // ✅ NUEVO: Calcular la fecha correcta basada en la última fecha de consumo
        LocalDate predictionDate = lastConsumptionDate.plusDays(i + 1);
        String dayOfWeek = predictionDate.getDayOfWeek().toString();
        
        if (waterDepleted) {
            // Water already depleted, set consumption to 0 for remaining days
            adjustedPredictions.add(new DailyPredictionDTO(
                predictionDate.toString(),  // ✅ Usar fecha calculada
                0.0,  // No consumption possible
                dayOfWeek
            ));
            log.debug("Day {}: {} - Water depleted, setting consumption to 0", i + 1, predictionDate);
            continue;
        }
        
        if (predictedConsumption <= remainingWater) {
            // Normal case: enough water for predicted consumption
            adjustedPredictions.add(new DailyPredictionDTO(
                predictionDate.toString(),  // ✅ Usar fecha calculada
                predictedConsumption,
                dayOfWeek
            ));
            remainingWater -= predictedConsumption;
            daysUntilRunout = i + 1;
            
            log.debug("Day {}: {} - Predicted {} L, Remaining {} L", 
                i + 1, predictionDate, predictedConsumption, remainingWater);
            
        } else {
            // Critical case: predicted consumption exceeds available water
            // Water will run out during this day
            adjustedPredictions.add(new DailyPredictionDTO(
                predictionDate.toString(),  // ✅ Usar fecha calculada
                remainingWater,  // Can only consume what's left
                dayOfWeek
            ));
            
            log.warn("Day {}: {} - Predicted {} L but only {} L available. Water will run out!", 
                i + 1, predictionDate, predictedConsumption, remainingWater);
            
            runoutDate = predictionDate;
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
            // ✅ CORREGIDO: Calcular desde la última fecha de consumo, no desde hoy
            runoutDate = lastConsumptionDate.plusDays(daysUntilRunout);
            
            log.info("Water will not run out in next 7 days. Estimated runout in {} additional days ({} total) on {}", 
                additionalDays, daysUntilRunout, runoutDate);
        } else {
            // No consumption, water never runs out
            daysUntilRunout = 999;
            runoutDate = lastConsumptionDate.plusYears(1);
            log.info("No consumption detected, water will not run out");
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
     * Gets the current water level from the most recent event for a subscription.
     * 
     * PRIMARY: Uses subscriptionId to get current water level.
     * 
     * @param subscriptionId The subscription ID
     * @param sensorId The sensor ID linked to the subscription
     * @param waterTankSize The tank size to convert percentage to liters
     * @return Current water level in liters
     */
    private Double getCurrentWaterLevel(Long subscriptionId, Long sensorId, Double waterTankSize) {
        try {
            // Get most recent events for this subscription (last 2 days to be safe)
            List<EventDTO> recentEvents = monitoringContextFacade.getEventsBySubscriptionId(
                subscriptionId,
                sensorId,
                LocalDate.now().minusDays(2),
                LocalDate.now()
            );

            if (recentEvents.isEmpty()) {
                log.warn("No recent events found for subscription: {}. Using default water level (50% of tank).", 
                    subscriptionId);
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

            log.debug("Latest event for subscription {} at {}: {}% = {} L", 
                subscriptionId, latestEvent.getTimestamp(), percentage, currentLiters);

            return currentLiters;

        } catch (Exception e) {
            log.error("Error getting current water level for subscription: {}", subscriptionId, e);
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
     * Builds the ML service request from consumption data.
     * 
     * @param subscriptionId The subscription ID (used for identification)
     * @param consumptions List of consumption records
     * @param currentWaterLevel Current water level in liters
     * @return ML prediction request
     */
    private MLPredictionRequest buildMLRequest(
            Long subscriptionId, 
            List<WaterConsumption> consumptions,
            Double currentWaterLevel) {

        // Filter out refill days
        List<WaterConsumption> normalConsumptions = consumptions.stream()
            .filter(wc -> !wc.getIsRefill())  // Exclude refill days
            .toList();
                
        log.info("Subscription {}: Total consumption records: {}, Normal: {}, Refills: {}", 
            subscriptionId,
            consumptions.size(), 
            normalConsumptions.size(), 
            consumptions.size() - normalConsumptions.size());   
        
        // Convert to historical data points (only normal consumption)
        var historicalData = normalConsumptions.stream()
            .map(wc -> new MLPredictionRequest.HistoricalDataPoint(
                wc.getDate().toString(),
                wc.getConsumption()
            ))
            .toList();

        return new MLPredictionRequest(
            subscriptionId.toString(),  // Use subscriptionId as identifier
            historicalData,
            currentWaterLevel
        );
    }

    /**
     * Creates a ConsumptionPrediction entity from ML response.
     * 
     * PRIMARY: Uses subscriptionId as primary identifier.
     * 
     * @param subscriptionId The subscription ID
     * @param residentId The resident ID (for secondary queries)
     * @param deviceId The device/sensor ID
     * @param mlResponse ML prediction response
     * @return New ConsumptionPrediction entity
     */
    private ConsumptionPrediction createPrediction(
            Long subscriptionId,
            Long residentId, 
            Long deviceId,
            MLPredictionResponse mlResponse) {
        
        try {
            String predictionsJson = objectMapper.writeValueAsString(
                mlResponse.getNext7DaysPredictions()
            );

            LocalDate runoutDate = LocalDate.parse(mlResponse.getWaterRunoutDate());

            return new ConsumptionPrediction(
                subscriptionId,  // PRIMARY identifier
                residentId,      // SECONDARY identifier
                deviceId,        // Device/sensor ID
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
     * Marks all active predictions for a subscription as OUTDATED.
     * 
     * PRIMARY: Uses subscriptionId to mark old predictions.
     * 
     * @param subscriptionId The subscription ID
     */
    private void markOldPredictionsAsOutdated(Long subscriptionId) {
        List<ConsumptionPrediction> activePredictions = 
            predictionRepository.findBySubscriptionIdAndStatus(
                subscriptionId, PredictionStatus.ACTIVE);

        activePredictions.forEach(ConsumptionPrediction::markAsOutdated);
        
        if (!activePredictions.isEmpty()) {
            predictionRepository.saveAll(activePredictions);
            log.info("Marked {} old predictions as OUTDATED for subscription: {}", 
                activePredictions.size(), subscriptionId);
        }
    }
}