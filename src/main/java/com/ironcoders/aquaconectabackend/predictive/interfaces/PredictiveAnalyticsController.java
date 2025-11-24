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
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionHistoryBySubscriptionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionCommandService;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionQueryService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.WaterConsumptionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.SubscriptionContextFacade;
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
@Tag(name = "Predictive Analytics")
//@PreAuthorize("isAuthenticated()")
public class PredictiveAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalyticsController.class);

    private final PredictionCommandService predictionCommandService;
    private final PredictionQueryService predictionQueryService;
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final SubscriptionContextFacade subscriptionContextFacade;

    /**
     * Constructor for dependency injection.
     */
    public PredictiveAnalyticsController(
            PredictionCommandService predictionCommandService,
            PredictionQueryService predictionQueryService,
            WaterConsumptionRepository waterConsumptionRepository,
            SubscriptionContextFacade subscriptionContextFacade) {
        this.predictionCommandService = predictionCommandService;
        this.predictionQueryService = predictionQueryService;
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * ✅ GET: ONLY retrieves the latest ACTIVE prediction (read-only operation).
     * Does NOT generate new predictions. Use POST endpoint to generate.
     * 
     * @param subscriptionId The subscription ID
     * @param residentId The resident ID (for authorization)
     * @return ResponseEntity with prediction resource or 404 if not found
     */
    @GetMapping("/subscriptions/{subscriptionId}/predictions")
    @Operation(
        summary = "Get latest prediction for a subscription",
        description = "Returns the most recent ACTIVE prediction. Does NOT generate a new one. Use POST to generate new predictions."
    )
    public ResponseEntity<PredictionResponseResource> getLatestPrediction(
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable Long subscriptionId,
            
            @Parameter(description = "Resident ID (for authorization)", required = true)
            @RequestParam Long residentId) {
        
        log.info("📖 GET: Fetching latest prediction for subscription: {} (resident: {})", 
            subscriptionId, residentId);

        try {
            // 1. Validate subscription ownership
            if (!subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId)) {
                log.warn("❌ Access denied: Subscription {} does not belong to resident {}", 
                    subscriptionId, residentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 2. Get latest ACTIVE prediction (read-only, no generation)
            Optional<ConsumptionPrediction> prediction = 
                predictionQueryService.handle(new GetLatestPredictionQuery(subscriptionId));

            if (prediction.isEmpty()) {
                log.info("📭 No active prediction found for subscription: {}", subscriptionId);
                return ResponseEntity.notFound().build();
            }

            // 3. Build response with refill info
            RefillInfoResource refillInfo = buildRefillInfo(subscriptionId);
            var resource = PredictionResourceFromEntityAssembler
                .toResourceFromEntity(prediction.get(), refillInfo);
            
            log.info("✅ Returning prediction generated at: {}", prediction.get().getPredictionDate());
            return ResponseEntity.ok(resource);

        } catch (Exception e) {
            log.error("❌ Error fetching prediction for subscription: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ✅ POST: ALWAYS generates a new prediction (write operation).
     * Marks previous predictions as OUTDATED.
     * 
     * @param subscriptionId The subscription ID
     * @param residentId The resident ID (for authorization)
     * @return ResponseEntity with newly generated prediction resource
     */
    @PostMapping("/subscriptions/{subscriptionId}/predictions")
    @Operation(
        summary = "Generate new prediction for a subscription",
        description = "Always generates a fresh prediction. Marks previous predictions as OUTDATED. Requires at least 7 days of consumption data."
    )
    public ResponseEntity<PredictionResponseResource> generatePrediction(
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable Long subscriptionId,
            
            @Parameter(description = "Resident ID (for authorization)", required = true)
            @RequestParam Long residentId) {
        
        log.info("🔮 POST: Generating NEW prediction for subscription: {} (resident: {})", 
            subscriptionId, residentId);

        try {
            // 1. Validate subscription ownership
            if (!subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId)) {
                log.warn("❌ Access denied: Subscription {} does not belong to resident {}", 
                    subscriptionId, residentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 2. Generate new prediction (always)
            var command = new GeneratePredictionCommand(subscriptionId, residentId);
            Optional<ConsumptionPrediction> prediction = 
                predictionCommandService.handle(command);

            if (prediction.isEmpty()) {
                log.warn("⚠️ Failed to generate prediction for subscription: {} (insufficient data or error)", 
                    subscriptionId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // 3. Build response with refill info
            RefillInfoResource refillInfo = buildRefillInfo(subscriptionId);
            var resource = PredictionResourceFromEntityAssembler
                .toResourceFromEntity(prediction.get(), refillInfo);
            
            log.info("✅ NEW prediction generated successfully for subscription: {}", subscriptionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(resource);

        } catch (Exception e) {
            log.error("❌ Error generating prediction for subscription: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 🆕 SECONDARY: Gets all predictions for a resident (across all subscriptions).
     * Useful for dashboard showing all water tanks.
     * 
     * @param residentId The resident ID
     * @return ResponseEntity with list of predictions
     */
    @GetMapping("/residents/{residentId}/predictions")
    @Operation(
        summary = "Get all predictions for a resident",
        description = "Returns latest ACTIVE prediction for each of the resident's active subscriptions (all water tanks)."
    )
    public ResponseEntity<List<PredictionResponseResource>> getAllPredictionsByResident(
            @Parameter(description = "Resident ID", required = true)
            @PathVariable Long residentId) {
        
        log.info("📚 GET: Fetching all predictions for resident: {}", residentId);

        try {
            // 1. Get all active subscriptions for this resident
            List<Long> subscriptionIds = subscriptionContextFacade
                .getActiveSubscriptionIdsByResident(residentId);

            if (subscriptionIds.isEmpty()) {
                log.info("📭 No active subscriptions found for resident: {}", residentId);
                return ResponseEntity.ok(List.of());
            }

            log.info("Found {} active subscriptions for resident: {}", subscriptionIds.size(), residentId);

            // 2. Get latest prediction for each subscription
            List<PredictionResponseResource> predictions = subscriptionIds.stream()
                .map(GetLatestPredictionQuery::new)
                .map(predictionQueryService::handle)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(prediction -> {
                    RefillInfoResource refillInfo = buildRefillInfo(prediction.getSubscriptionId());
                    return PredictionResourceFromEntityAssembler
                        .toResourceFromEntity(prediction, refillInfo);
                })
                .collect(Collectors.toList());

            log.info("✅ Returning {} predictions for resident: {}", predictions.size(), residentId);
            return ResponseEntity.ok(predictions);

        } catch (Exception e) {
            log.error("❌ Error fetching predictions for resident: {}", residentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 🆕 PROVIDER: Gets complete prediction history for a subscription (all statuses).
     * Returns ALL predictions (ACTIVE + OUTDATED) ordered by date descending.
     * Useful for providers to track prediction changes over time.
     * 
     * @param subscriptionId The subscription ID
     * @return ResponseEntity with list of all predictions
     */
    @GetMapping("/subscriptions/{subscriptionId}/predictions/history")
    @Operation(
        summary = "Get complete prediction history for a subscription",
        description = "Returns ALL predictions (both ACTIVE and OUTDATED status) for a subscription, ordered by prediction date (newest first). This allows providers to track prediction evolution over time."
    )
    public ResponseEntity<List<PredictionResponseResource>> getPredictionHistory(
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable Long subscriptionId) {
        
        log.info("📜 GET: Fetching complete prediction history for subscription: {}", subscriptionId);

        try {
            // Get all predictions regardless of status
            var query = new GetPredictionHistoryBySubscriptionQuery(subscriptionId);
            List<ConsumptionPrediction> predictions = predictionQueryService.handle(query);

            if (predictions.isEmpty()) {
                log.info("📭 No prediction history found for subscription: {}", subscriptionId);
                return ResponseEntity.ok(List.of());
            }

            // Build refill info once (current state)
            RefillInfoResource refillInfo = buildRefillInfo(subscriptionId);

            // Transform all predictions to resources
            List<PredictionResponseResource> resources = predictions.stream()
                .map(prediction -> PredictionResourceFromEntityAssembler
                    .toResourceFromEntity(prediction, refillInfo))
                .collect(Collectors.toList());

            log.info("✅ Returning {} predictions (history) for subscription: {}", 
                resources.size(), subscriptionId);
            
            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("❌ Error fetching prediction history for subscription: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PRIMARY: Gets consumption history for a subscription in a date range.
     * 
     * @param subscriptionId The subscription ID
     * @param startDate Start date (optional, defaults to 30 days ago)
     * @param endDate End date (optional, defaults to today)
     * @return ResponseEntity with list of consumption history resources
     */
    @GetMapping("/subscriptions/{subscriptionId}/consumption-history")
    @Operation(
        summary = "Get consumption history for a subscription",
        description = "Gets historical water consumption data for a specific subscription in a specified date range."
    )
    public ResponseEntity<List<ConsumptionHistoryResource>> getConsumptionHistory(
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable Long subscriptionId,
            
            @Parameter(description = "Start date (format: yyyy-MM-dd)", example = "2025-10-01")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate startDate,
            
            @Parameter(description = "End date (format: yyyy-MM-dd)", example = "2025-10-31")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate endDate) {
        
        log.info("Received consumption history request for subscription: {}", subscriptionId);

        try {
            // Default values if not provided
            LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();
            LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusDays(30);

            // Query consumption history
            var query = new GetConsumptionHistoryQuery(subscriptionId, finalStartDate, finalEndDate);
            List<WaterConsumption> consumptions = predictionQueryService.handle(query);

            if (consumptions.isEmpty()) {
                log.info("No consumption history found for subscription: {}", subscriptionId);
                return ResponseEntity.noContent().build();
            }

            // Transform to resources
            List<ConsumptionHistoryResource> resources = consumptions.stream()
                .map(ConsumptionResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());

            log.info("Returning {} consumption records for subscription: {}", 
                resources.size(), subscriptionId);
            
            return ResponseEntity.ok(resources);

        } catch (Exception e) {
            log.error("Error retrieving consumption history for subscription: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PRIMARY: Builds refill information for a subscription.
     * 
     * @param subscriptionId The subscription ID
     * @return RefillInfoResource with refill statistics
     */
    private RefillInfoResource buildRefillInfo(Long subscriptionId) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            // Count refills in last 30 days for this subscription
            Long refillCount = waterConsumptionRepository
                .countBySubscriptionIdAndDateBetweenAndIsRefillTrue(
                    subscriptionId, startDate, endDate);
            
            // Get last refill date for this subscription
            Optional<WaterConsumption> lastRefill = waterConsumptionRepository
                .findFirstBySubscriptionIdAndIsRefillTrueOrderByDateDesc(subscriptionId);
            
            String lastRefillStr = lastRefill
                .map(wc -> wc.getDate().toString())
                .orElse(null);
            
            Integer daysSinceRefill = lastRefill
                .map(wc -> (int) java.time.temporal.ChronoUnit.DAYS.between(wc.getDate(), LocalDate.now()))
                .orElse(null);
            
            log.debug("Refill info for subscription {}: {} refills in last 30 days, last refill: {}", 
                subscriptionId, refillCount, lastRefillStr);
            
            return new RefillInfoResource(
                refillCount.intValue(),
                lastRefillStr,
                daysSinceRefill
            );
            
        } catch (Exception e) {
            log.error("Error building refill info for subscription: {}", subscriptionId, e);
            // Return empty refill info on error
            return new RefillInfoResource(0, null, null);
        }
    }
}