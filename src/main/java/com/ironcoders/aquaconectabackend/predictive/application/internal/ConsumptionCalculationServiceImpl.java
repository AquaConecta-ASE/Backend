package com.ironcoders.aquaconectabackend.predictive.application.internal;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.services.ConsumptionCalculationService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.WaterConsumptionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.MonitoringContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.SubscriptionContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto.EventDTO;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsumptionCalculationServiceImpl implements ConsumptionCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ConsumptionCalculationServiceImpl.class);

    private final WaterConsumptionRepository waterConsumptionRepository;
    private final MonitoringContextFacade monitoringContextFacade;
    private final SubscriptionContextFacade subscriptionContextFacade;

    /**
     * Constructor for dependency injection.
     */
    public ConsumptionCalculationServiceImpl(
            WaterConsumptionRepository waterConsumptionRepository,
            MonitoringContextFacade monitoringContextFacade,
            SubscriptionContextFacade subscriptionContextFacade) {
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.monitoringContextFacade = monitoringContextFacade;
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * Calculates and persists daily consumption from monitoring events.
     * 
     * Now handles percentage-based level values:
     * - levelValue is a percentage (0-100)
     * - Converts to liters using waterTankSize
     * - Formula: liters = (percentage / 100) * waterTankSize
     * 
     * PRIMARY: Uses subscriptionId to calculate consumption for a specific subscription.
     * 
     * @param command The command with subscriptionId, residentId, and date range
     * @return List of calculated and persisted WaterConsumption records
     */
    @Override
    @Transactional
    public List<WaterConsumption> handle(CalculateDailyConsumptionCommand command) {
        try {
            log.info("Starting consumption calculation for subscription: {} (resident: {}) from {} to {}", 
                command.subscriptionId(), command.residentId(), command.startDate(), command.endDate());

            // 1. Validate command
            command.validate();

            // 2. Validate subscription belongs to resident
            if (!subscriptionContextFacade.isSubscriptionOwnedByResident(
                    command.subscriptionId(), command.residentId())) {
                log.error("Subscription {} does not belong to resident {}", 
                    command.subscriptionId(), command.residentId());
                return Collections.emptyList();
            }

            // 3. Get sensor ID for this subscription
            Optional<Long> sensorIdOpt = subscriptionContextFacade.getSensorIdBySubscription(
                command.subscriptionId());
            
            if (sensorIdOpt.isEmpty()) {
                log.error("No sensor found for subscription: {}", command.subscriptionId());
                return Collections.emptyList();
            }
            
            Long sensorId = sensorIdOpt.get();

            // 4. Get water tank size (needed to convert percentage to liters)
            Double waterTankSize = subscriptionContextFacade
                .getWaterTankSizeBySubscription(command.subscriptionId())
                .orElse(1000.0); // Default 1000L if not found
            
            log.info("Subscription {}: sensor={}, tankSize={} liters", 
                command.subscriptionId(), sensorId, waterTankSize);

            // 5. Get events from Monitoring BC (via ACL) - using subscription context
            List<EventDTO> events = monitoringContextFacade.getEventsBySubscriptionId(
                command.subscriptionId(),
                sensorId,
                command.startDate(),
                command.endDate()
            );

            if (events.isEmpty()) {
                log.warn("No events found for subscription: {} in date range", command.subscriptionId());
                return Collections.emptyList();
            }

            log.info("Found {} events for subscription: {}", events.size(), command.subscriptionId());

            // 6. Group events by date
            Map<LocalDate, List<EventDTO>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(EventDTO::getDate));

            // 7. Calculate consumption for each day (with update support)
            List<WaterConsumption> consumptionsToSave = new ArrayList<>();
            int newCount = 0;
            int updatedCount = 0;

            for (Map.Entry<LocalDate, List<EventDTO>> entry : eventsByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<EventDTO> dayEvents = entry.getValue();

                // Check if consumption already exists for this day (using subscriptionId)
                Optional<WaterConsumption> existingConsumption = 
                    waterConsumptionRepository.findBySubscriptionIdAndDate(
                        command.subscriptionId(), date);

                // Sort events by timestamp to get first and last
                dayEvents.sort(Comparator.comparing(EventDTO::getTimestamp));
                
                if (dayEvents.isEmpty()) {
                    log.warn("No events for date: {}", date);
                    continue;
                }

                EventDTO firstEvent = dayEvents.get(0);
                EventDTO lastEvent = dayEvents.get(dayEvents.size() - 1);

                // Parse levels and convert to liters
                Double initialPercentage = parsePercentage(firstEvent.getLevelValue());
                Double finalPercentage = parsePercentage(lastEvent.getLevelValue());
                Double initialLiters = (initialPercentage / 100.0) * waterTankSize;
                Double finalLiters = (finalPercentage / 100.0) * waterTankSize;
                String waterQuality = getMostCommonQuality(dayEvents);
                Long deviceId = firstEvent.getDeviceId();

                // 🔍 NUEVO: Detect refill by checking intermediate events
                boolean hasRefill = detectRefillInEvents(dayEvents, waterTankSize);

                if (existingConsumption.isPresent()) {
                    // UPDATE existing consumption
                    WaterConsumption consumption = existingConsumption.get();
                    
                    // Update basic data
                    consumption.updateConsumptionData(initialLiters, finalLiters, waterQuality, deviceId);
                    
                    // Override refill status based on intermediate events analysis
                    if (hasRefill) {
                        consumption.setRefillStatus(true);
                        if (!consumption.getIsRefill()) {
                            log.info("🔄 REFILL NOW DETECTED for date {} (detected through intermediate events)", date);
                        }
                    }
                    
                    consumptionsToSave.add(consumption);
                    updatedCount++;
                    log.info("♻️ Updated consumption for subscription: {} on date: {} (refill: {})", 
                        command.subscriptionId(), date, hasRefill);
                } else {
                    // CREATE new consumption
                    Optional<WaterConsumption> newConsumption = calculateDailyConsumption(
                        command.subscriptionId(),
                        command.residentId(),
                        date,
                        dayEvents,
                        waterTankSize
                    );
                    
                    if (newConsumption.isPresent()) {
                        consumptionsToSave.add(newConsumption.get());
                        newCount++;
                        log.info("✨ Created new consumption for subscription: {} on date: {}", 
                            command.subscriptionId(), date);
                    }
                }
            }

            // 8. Save all consumption records (new + updated)
            if (!consumptionsToSave.isEmpty()) {
                waterConsumptionRepository.saveAll(consumptionsToSave);
                log.info("💾 Saved {} consumption records for subscription: {} (new: {}, updated: {})", 
                    consumptionsToSave.size(), command.subscriptionId(), newCount, updatedCount);
            } else {
                log.info("No consumption records to save for subscription: {}", command.subscriptionId());
            }

            // 9. Return all consumption data (existing + new) for the date range
            return waterConsumptionRepository.findBySubscriptionIdAndDateBetweenOrderByDateAsc(
                command.subscriptionId(),
                command.startDate(),
                command.endDate()
            );

        } catch (Exception e) {
            log.error("Error calculating consumption for subscription: {}", command.subscriptionId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Calculates consumption for a single day from events.
     * 
     * NEW LOGIC:
     * - levelValue is a PERCENTAGE (e.g., "85" = 85%)
     * - Convert to liters: liters = (percentage / 100) * waterTankSize
     * - Calculate consumption: consumption = initialLiters - finalLiters
     * 
     * PRIMARY: Uses subscriptionId for consumption tracking.
     * 
     * @param subscriptionId The subscription ID
     * @param residentId The resident ID (for logging)
     * @param date The date to calculate
     * @param events List of events for that day
     * @param waterTankSize Capacity of the water tank in liters
     * @return Optional containing the calculated WaterConsumption
     */
    private Optional<WaterConsumption> calculateDailyConsumption(
            Long subscriptionId,
            Long residentId,
            LocalDate date,
            List<EventDTO> events,
            Double waterTankSize) {
        
        if (events.isEmpty()) {
            return Optional.empty();
        }

        // Sort events by timestamp to get first and last
        events.sort(Comparator.comparing(EventDTO::getTimestamp));

        // Get initial and final water levels
        EventDTO firstEvent = events.get(0);
        EventDTO lastEvent = events.get(events.size() - 1);

        try {
            // Parse levelValue as PERCENTAGE
            Double initialPercentage = parsePercentage(firstEvent.getLevelValue());
            Double finalPercentage = parsePercentage(lastEvent.getLevelValue());

            // Convert percentage to liters
            Double initialLiters = (initialPercentage / 100.0) * waterTankSize;
            Double finalLiters = (finalPercentage / 100.0) * waterTankSize;

            log.debug("Date: {}, Levels: {}% → {}% ({} L → {} L)", 
                date, initialPercentage, finalPercentage, initialLiters, finalLiters);

            // NUEVO: Detect if there was a refill
            boolean isRefill = finalLiters > initialLiters;

            //  NUEVO: Calculate consumption differently based on refill
            Double consumption;
            if (isRefill) {
                // Water was refilled, set consumption to 0
                // (We don't count refills as consumption)
                consumption = 0.0;

                log.info("🔄 REFILL detected for subscription {} (resident {}) on {}: {}% → {}% ({}L → {}L)",
                    subscriptionId, residentId, date, initialPercentage, finalPercentage, 
                    initialLiters, finalLiters);
            } else {
                // Normal consumption: water level decreased
                consumption = Math.abs(initialLiters - finalLiters);

                log.debug("✅ Normal consumption for subscription {} (resident {}) on {}: {} L ({}% to {}%)",
                    subscriptionId, residentId, date, consumption, initialPercentage, finalPercentage);
            }

            // Get most common water quality for the day
            String waterQuality = getMostCommonQuality(events);

            // Get device ID from first event
            Long deviceId = firstEvent.getDeviceId();

            // Create WaterConsumption entity with subscriptionId
            WaterConsumption waterConsumption = new WaterConsumption(
                subscriptionId,   // PRIMARY identifier
                residentId,       // SECONDARY identifier (for cross-subscription queries)
                date,
                consumption,
                deviceId,
                initialLiters,    // Store in liters for clarity
                finalLiters,      // Store in liters for clarity
                waterQuality,
                isRefill          // NUEVO: marcar si fue un refill
            );

            log.debug("Calculated consumption for subscription: {} on {}: {} L ({}% to {}% of {} L tank)",
                subscriptionId, date, consumption, initialPercentage, finalPercentage, waterTankSize);

            return Optional.of(waterConsumption);

        } catch (Exception e) {
            log.error("Error calculating consumption for date: {}", date, e);
            return Optional.empty();
        }
    }

    /**
     * Parses level value as PERCENTAGE.
     * Expects values between 0 and 100.
     * 
     * Examples:
     * - "85" → 85.0
     * - "92.5" → 92.5
     * - "100" → 100.0
     */
    private Double parsePercentage(String levelValue) {
        try {
            // Remove any non-numeric characters except decimal point
            String cleaned = levelValue.replaceAll("[^0-9.]", "");
            Double percentage = Double.parseDouble(cleaned);
            
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
     * Gets the most common water quality value for the day.
     */
    private String getMostCommonQuality(List<EventDTO> events) {
        return events.stream()
            .map(EventDTO::getQualityValue)
            .collect(Collectors.groupingBy(q -> q, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }

    /**
     * Detects if there was a refill during the day by analyzing all events.
     * A refill is detected when there's a significant increase in water level
     * between consecutive events (>= 50% increase).
     * 
     * This handles cases where:
     * - Initial events showed normal consumption (68% → 62%)
     * - Later, intermediate events arrive showing the full story (68% → 20% → 100% → 62%)
     * 
     * @param events List of events for the day (must be sorted by timestamp)
     * @param waterTankSize Tank capacity to convert percentages to liters
     * @return true if a refill was detected, false otherwise
     */
    private boolean detectRefillInEvents(List<EventDTO> events, Double waterTankSize) {
        if (events.size() < 2) {
            return false;
        }

        // Ensure events are sorted by timestamp
        events.sort(Comparator.comparing(EventDTO::getTimestamp));

        // Check consecutive events for significant level increases
        for (int i = 0; i < events.size() - 1; i++) {
            EventDTO currentEvent = events.get(i);
            EventDTO nextEvent = events.get(i + 1);

            Double currentPercentage = parsePercentage(currentEvent.getLevelValue());
            Double nextPercentage = parsePercentage(nextEvent.getLevelValue());

            // Calculate increase
            Double percentageIncrease = nextPercentage - currentPercentage;

            // If water level increased by 50% or more, it's a refill
            if (percentageIncrease >= 50.0) {
                log.info("🔍 Refill detected between events: {}% → {}% (increase: {}%) at {}",
                    currentPercentage, nextPercentage, percentageIncrease, 
                    nextEvent.getTimestamp());
                return true;
            }
        }

        return false;
    }
}