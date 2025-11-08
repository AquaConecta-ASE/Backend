package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Facade for accessing Subscription bounded context.
 * Uses JdbcTemplate for direct database access to avoid circular dependencies
 * and ensure reliable access to subscription data.
 */
@Service("predictiveSubscriptionContextFacade")
public class SubscriptionContextFacade {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionContextFacade.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor for dependency injection.
     */
    public SubscriptionContextFacade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * DEPRECATED: Gets the water tank size for a resident (first active subscription).
     * Use getWaterTankSizeBySubscription() instead.
     *
     * @param residentId The resident ID
     * @return Optional containing the water tank size in liters
     */
    @Deprecated
    public Optional<Double> getWaterTankSizeByResidentId(Long residentId) {
        log.debug("Fetching water tank size for resident: {} (DEPRECATED)", residentId);

        try {
            String sql = "SELECT water_tank_size FROM subscriptions " +
                        "WHERE resident_id = ? AND status = 'ACTIVE' LIMIT 1";
            Double tankSize = jdbcTemplate.queryForObject(sql, Double.class, residentId);
            
            log.debug("Water tank size for resident {}: {} liters", residentId, tankSize);
            return Optional.ofNullable(tankSize);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No active subscription found for resident: {}", residentId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching water tank size for resident: {}", residentId, e);
            return Optional.empty();
        }
    }

    /**
     * DEPRECATED: Gets the sensor/device ID from the subscription.
     * Use getSensorIdBySubscription() instead.
     *
     * @param residentId The resident ID
     * @return Optional containing the sensor ID
     */
    @Deprecated
    public Optional<Long> getSensorIdByResidentId(Long residentId) {
        log.debug("Fetching sensor ID for resident: {} (DEPRECATED)", residentId);

        try {
            String sql = "SELECT sensor_id FROM subscriptions " +
                        "WHERE resident_id = ? AND status = 'ACTIVE' LIMIT 1";
            Long sensorId = jdbcTemplate.queryForObject(sql, Long.class, residentId);
            
            log.debug("Sensor ID for resident {}: {}", residentId, sensorId);
            return Optional.ofNullable(sensorId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No active subscription found for resident: {}", residentId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching sensor ID for resident: {}", residentId, e);
            return Optional.empty();
        }
    }

    /**
     * DEPRECATED: Checks if a resident has an active subscription.
     *
     * @param residentId The resident ID
     * @return true if resident has active subscription, false otherwise
     */
    @Deprecated
    public boolean hasActiveSubscription(Long residentId) {
        try {
            String sql = "SELECT COUNT(*) FROM subscriptions WHERE resident_id = ? AND status = 'ACTIVE'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, residentId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Error checking active subscription for resident: {}", residentId, e);
            return false;
        }
    }

    // ====== PRIMARY METHODS FOR MULTI-SUBSCRIPTION SUPPORT ======

    /**
     * PRIMARY: Gets subscription information by ID.
     *
     * @param subscriptionId The subscription ID
     * @return Optional containing SubscriptionInfo DTO
     */
    public Optional<SubscriptionInfo> getSubscriptionById(Long subscriptionId) {
        log.debug("Fetching subscription by ID: {}", subscriptionId);

        try {
            String sql = """
                SELECT id, resident_id, sensor_id, water_tank_size, status, 
                       provider_id, start_date, end_date
                FROM subscriptions 
                WHERE id = ?
                """;
            
            SubscriptionInfo info = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> 
                new SubscriptionInfo(
                    rs.getLong("id"),
                    rs.getLong("resident_id"),
                    rs.getLong("sensor_id"),
                    rs.getDouble("water_tank_size"),
                    rs.getString("status"),
                    rs.getLong("provider_id"),
                    rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                    rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null
                ), subscriptionId);
            
            log.debug("Found subscription info: {}", info);
            return Optional.ofNullable(info);
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("Subscription not found: {}", subscriptionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching subscription by ID: {}", subscriptionId, e);
            return Optional.empty();
        }
    }

    /**
     * PRIMARY: Gets the resident ID for a given subscription.
     *
     * @param subscriptionId The subscription ID
     * @return Optional containing the resident ID
     */
    public Optional<Long> getResidentIdBySubscription(Long subscriptionId) {
        log.debug("Fetching resident ID for subscription: {}", subscriptionId);

        try {
            String sql = "SELECT resident_id FROM subscriptions WHERE id = ?";
            Long residentId = jdbcTemplate.queryForObject(sql, Long.class, subscriptionId);
            
            log.debug("Found residentId: {} for subscriptionId: {}", residentId, subscriptionId);
            return Optional.ofNullable(residentId);
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("Subscription not found: {}", subscriptionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching residentId for subscriptionId: {}", subscriptionId, e);
            return Optional.empty();
        }
    }

    /**
     * PRIMARY: Gets the sensor ID for a given subscription.
     *
     * @param subscriptionId The subscription ID
     * @return Optional containing the sensor ID
     */
    public Optional<Long> getSensorIdBySubscription(Long subscriptionId) {
        log.debug("Fetching sensor ID for subscription: {}", subscriptionId);

        try {
            String sql = "SELECT sensor_id FROM subscriptions WHERE id = ?";
            Long sensorId = jdbcTemplate.queryForObject(sql, Long.class, subscriptionId);
            
            log.debug("Found sensorId: {} for subscriptionId: {}", sensorId, subscriptionId);
            return Optional.ofNullable(sensorId);
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("Subscription not found: {}", subscriptionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching sensorId for subscriptionId: {}", subscriptionId, e);
            return Optional.empty();
        }
    }

    /**
     * PRIMARY: Gets the water tank size for a given subscription.
     *
     * @param subscriptionId The subscription ID
     * @return Optional containing the water tank size in liters
     */
    public Optional<Double> getWaterTankSizeBySubscription(Long subscriptionId) {
        log.debug("Fetching water tank size for subscription: {}", subscriptionId);

        try {
            String sql = "SELECT water_tank_size FROM subscriptions WHERE id = ?";
            Double waterTankSize = jdbcTemplate.queryForObject(sql, Double.class, subscriptionId);
            
            log.debug("Found waterTankSize: {} for subscriptionId: {}", waterTankSize, subscriptionId);
            return Optional.ofNullable(waterTankSize);
            
        } catch (EmptyResultDataAccessException e) {
            log.warn("Subscription not found: {}", subscriptionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching waterTankSize for subscriptionId: {}", subscriptionId, e);
            return Optional.empty();
        }
    }

    /**
     * PRIMARY: Validates that a subscription belongs to a specific resident.
     *
     * @param subscriptionId The subscription ID
     * @param residentId The resident ID
     * @return true if the subscription belongs to the resident, false otherwise
     */
    public boolean isSubscriptionOwnedByResident(Long subscriptionId, Long residentId) {
        log.debug("Validating subscription {} belongs to resident {}", subscriptionId, residentId);

        try {
            String sql = "SELECT COUNT(*) FROM subscriptions WHERE id = ? AND resident_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, subscriptionId, residentId);
            
            boolean isOwned = count != null && count > 0;
            log.debug("Subscription {} owned by resident {}: {}", subscriptionId, residentId, isOwned);
            
            return isOwned;
            
        } catch (Exception e) {
            log.error("Error validating subscription {} for resident {}", subscriptionId, residentId, e);
            return false;
        }
    }

    /**
     * SECONDARY: Gets all active subscription IDs for a resident.
     *
     * @param residentId The resident ID
     * @return List of active subscription IDs
     */
    public List<Long> getActiveSubscriptionIdsByResident(Long residentId) {
        log.debug("Fetching active subscription IDs for resident: {}", residentId);

        try {
            String sql = "SELECT id FROM subscriptions WHERE resident_id = ? AND status = 'ACTIVE'";
            List<Long> subscriptionIds = jdbcTemplate.queryForList(sql, Long.class, residentId);
            
            log.debug("Found {} active subscriptions for resident: {}", subscriptionIds.size(), residentId);
            return subscriptionIds;
            
        } catch (Exception e) {
            log.error("Error fetching active subscriptions for resident: {}", residentId, e);
            return List.of();
        }
    }

    /**
     * Checks if a subscription is active.
     *
     * @param subscriptionId The subscription ID
     * @return true if subscription is active, false otherwise
     */
    public boolean isSubscriptionActive(Long subscriptionId) {
        log.debug("Checking if subscription {} is active", subscriptionId);

        try {
            String sql = "SELECT COUNT(*) FROM subscriptions WHERE id = ? AND status = 'ACTIVE'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, subscriptionId);
            
            boolean isActive = count != null && count > 0;
            log.debug("Subscription {} active: {}", subscriptionId, isActive);
            
            return isActive;
            
        } catch (Exception e) {
            log.error("Error checking if subscription {} is active", subscriptionId, e);
            return false;
        }
    }

    /**
     * DTO for subscription information
     */
    public record SubscriptionInfo(
        Long id,
        Long residentId,
        Long sensorId,
        Double waterTankSize,
        String status,
        Long providerId,
        LocalDate startDate,
        LocalDate endDate
    ) {}
}