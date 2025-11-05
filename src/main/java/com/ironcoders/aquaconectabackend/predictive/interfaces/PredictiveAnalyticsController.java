package com.ironcoders.aquaconectabackend.predictive.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.GeneratePredictionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetConsumptionHistoryQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetLatestPredictionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionCommandService;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionQueryService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.WaterConsumptionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.ConsumptionHistoryResource;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.PredictionResponseResource;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.RefillInfoResource;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.transform.ConsumptionResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.transform.PredictionResourceFromEntityAssembler;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/predictive-analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Predictive Analytics", description = "Water Consumption Prediction and Analytics Endpoints")
//@PreAuthorize("isAuthenticated()")
public class PredictiveAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalyticsController.class);

    private final PredictionCommandService predictionCommandService;
    private final PredictionQueryService predictionQueryService;
    private final WaterConsumptionRepository waterConsumptionRepository;

    /**
     * Constructor for dependency injection.
     */
    public PredictiveAnalyticsController(
            PredictionCommandService predictionCommandService,
            PredictionQueryService predictionQueryService,
            WaterConsumptionRepository waterConsumptionRepository) {
        this.predictionCommandService = predictionCommandService;
        this.predictionQueryService = predictionQueryService;
        this.waterConsumptionRepository = waterConsumptionRepository;
    }

    /**
     * Gets consumption prediction for a resident.
     * Returns cached prediction if valid (less than 24h old), otherwise generates new one.
     * 
     * @param residentId The resident ID
     * @return ResponseEntity with prediction resource
     */
    @GetMapping("/residents/{residentId}/predictions")
    @Operation(
        summary = "Get consumption prediction",
        description = "Gets water consumption prediction for the next 7 days. Returns cached prediction if available and valid (less than 24h old)."
    )
    public ResponseEntity<PredictionResponseResource> getPrediction(
            @Parameter(description = "Resident ID", required = true)
            @PathVariable Long residentId) {
        
        log.info("Received prediction request for resident: {}", residentId);

        try {
            // 1. Try to get valid cached prediction
            Optional<ConsumptionPrediction> cachedPrediction = 
                predictionQueryService.handle(new GetLatestPredictionQuery(residentId));

            if (cachedPrediction.isPresent()) {
                log.info("Returning cached prediction for resident: {}", residentId);
                
                // ✅ NUEVO: Build refill info
                RefillInfoResource refillInfo = buildRefillInfo(residentId);
                
                var resource = PredictionResourceFromEntityAssembler
                    .toResourceFromEntity(cachedPrediction.get(), refillInfo);  // ✅ ACTUALIZADO
                return ResponseEntity.ok(resource);
            }

            // 2. No valid cache, generate new prediction
            log.info("No valid cached prediction found. Generating new prediction for resident: {}", 
                residentId);

            var command = new GeneratePredictionCommand(residentId);
            Optional<ConsumptionPrediction> newPrediction = 
                predictionCommandService.handle(command);

            if (newPrediction.isEmpty()) {
                log.warn("Failed to generate prediction for resident: {}", residentId);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
            }

            // ✅ NUEVO: Build refill info
            RefillInfoResource refillInfo = buildRefillInfo(residentId);

            // 3. Return new prediction
            var resource = PredictionResourceFromEntityAssembler
                .toResourceFromEntity(newPrediction.get(), refillInfo);  // ✅ ACTUALIZADO
            
            return ResponseEntity.status(HttpStatus.CREATED).body(resource);

        } catch (Exception e) {
            log.error("Error processing prediction request for resident: {}", residentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Forces generation of a new prediction (bypasses cache).
     * Useful for manual refresh or testing.
     * 
     * @param residentId The resident ID
     * @return ResponseEntity with new prediction resource
     */
    @PostMapping("/residents/{residentId}/predictions")
    @Operation(
        summary = "Generate new prediction",
        description = "Forces generation of a new prediction, bypassing any cached results."
    )
    public ResponseEntity<PredictionResponseResource> generatePrediction(
            @Parameter(description = "Resident ID", required = true)
            @PathVariable Long residentId) {
        
        log.info("Received force-generate prediction request for resident: {}", residentId);

        try {
            var command = new GeneratePredictionCommand(residentId);
            Optional<ConsumptionPrediction> prediction = 
                predictionCommandService.handle(command);

            if (prediction.isEmpty()) {
                log.warn("Failed to generate prediction for resident: {}", residentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // ✅ NUEVO: Build refill info
            RefillInfoResource refillInfo = buildRefillInfo(residentId);

            var resource = PredictionResourceFromEntityAssembler
                .toResourceFromEntity(prediction.get(), refillInfo);  // ✅ ACTUALIZADO
            
            return ResponseEntity.status(HttpStatus.CREATED).body(resource);

        } catch (Exception e) {
            log.error("Error generating prediction for resident: {}", residentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets consumption history for a resident in a date range.
     * 
     * @param residentId The resident ID
     * @param startDate Start date (optional, defaults to 30 days ago)
     * @param endDate End date (optional, defaults to today)
     * @return ResponseEntity with list of consumption history resources
     */
    @GetMapping("/residents/{residentId}/consumption-history")
    @Operation(
        summary = "Get consumption history",
        description = "Gets historical water consumption data for a resident in a specified date range."
    )
    public ResponseEntity<List<ConsumptionHistoryResource>> getConsumptionHistory(
            @Parameter(description = "Resident ID", required = true)
            @PathVariable Long residentId,
            
            @Parameter(description = "Start date (format: yyyy-MM-dd)", example = "2025-10-01")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate startDate,
            
            @Parameter(description = "End date (format: yyyy-MM-dd)", example = "2025-10-31")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate endDate) {
        
        log.info("Received consumption history request for resident: {}", residentId);

        try {
            // Default values if not provided
            LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();
            LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusDays(30);

            // Query consumption history
            var query = new GetConsumptionHistoryQuery(residentId, finalStartDate, finalEndDate);
            List<WaterConsumption> consumptions = predictionQueryService.handle(query);

            if (consumptions.isEmpty()) {
                log.info("No consumption history found for resident: {}", residentId);
                return ResponseEntity.noContent().build();
            }

            // Transform to resources
            List<ConsumptionHistoryResource> resources = consumptions.stream()
                .map(ConsumptionResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());

            log.info("Returning {} consumption records for resident: {}", 
                resources.size(), residentId);
            
            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("Error retrieving consumption history for resident: {}", residentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ NUEVO MÉTODO: Builds refill information for the response
    /**
     * Builds refill information for a resident.
     * 
     * @param residentId The resident ID
     * @return RefillInfoResource with refill statistics
     */
    private RefillInfoResource buildRefillInfo(Long residentId) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            // Count refills in last 30 days
            Long refillCount = waterConsumptionRepository
                .countByResidentIdAndDateBetweenAndIsRefillTrue(residentId, startDate, endDate);
            
            // Get last refill date
            Optional<WaterConsumption> lastRefill = waterConsumptionRepository
                .findFirstByResidentIdAndIsRefillTrueOrderByDateDesc(residentId);
            
            String lastRefillStr = lastRefill
                .map(wc -> wc.getDate().toString())
                .orElse(null);
            
            Integer daysSinceRefill = lastRefill
                .map(wc -> (int) java.time.temporal.ChronoUnit.DAYS.between(wc.getDate(), LocalDate.now()))
                .orElse(null);
            
            log.debug("Refill info for resident {}: {} refills in last 30 days, last refill: {}", 
                residentId, refillCount, lastRefillStr);
            
            return new RefillInfoResource(
                refillCount.intValue(),
                lastRefillStr,
                daysSinceRefill
            );
            
        } catch (Exception e) {
            log.error("Error building refill info for resident: {}", residentId, e);
            // Return empty refill info on error
            return new RefillInfoResource(0, null, null);
        }
    }
}