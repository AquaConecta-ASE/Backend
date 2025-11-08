package com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterConsumptionRepository extends JpaRepository<WaterConsumption, Long> {

    // ====== PRIMARY QUERIES (by subscriptionId) ======
    
    /**
     * Find all consumption records for a subscription
     */
    List<WaterConsumption> findBySubscriptionIdOrderByDateAsc(Long subscriptionId);

    /**
     * Find consumption records for a subscription within a date range
     */
    List<WaterConsumption> findBySubscriptionIdAndDateBetweenOrderByDateAsc(
            Long subscriptionId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find consumption for a specific subscription and date
     */
    Optional<WaterConsumption> findBySubscriptionIdAndDate(Long subscriptionId, LocalDate date);

    /**
     * Check if consumption record exists for a subscription on a date
     */
    boolean existsBySubscriptionIdAndDate(Long subscriptionId, LocalDate date);

    /**
     * Find consumption records excluding refill days for a subscription
     */
    List<WaterConsumption> findBySubscriptionIdAndDateBetweenAndIsRefillFalseOrderByDateAsc(
            Long subscriptionId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find only refill days for a subscription
     */
    List<WaterConsumption> findBySubscriptionIdAndIsRefillTrueOrderByDateDesc(Long subscriptionId);

    /**
     * Count refills in a date range for a subscription
     */
    Long countBySubscriptionIdAndDateBetweenAndIsRefillTrue(
            Long subscriptionId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find the most recent refill day for a subscription
     */
    Optional<WaterConsumption> findFirstBySubscriptionIdAndIsRefillTrueOrderByDateDesc(Long subscriptionId);

    // ====== SECONDARY QUERIES (by residentId - for getting all data across subscriptions) ======
    
    /**
     * Find all consumption records for a resident (across all subscriptions)
     */
    List<WaterConsumption> findByResidentId(Long residentId);

    /**
     * Find consumption records for a resident ordered by date
     * DEPRECATED - use subscription-based query for specific predictions
     */
    @Deprecated
    List<WaterConsumption> findByResidentIdOrderByDateAsc(Long residentId);

    /**
     * Find consumption records for a resident within a date range
     * DEPRECATED - use subscription-based query for specific predictions
     */
    @Deprecated
    List<WaterConsumption> findByResidentIdAndDateBetweenOrderByDateAsc(
            Long residentId,
            LocalDate startDate,
            LocalDate endDate
    );

    // ====== UTILITY QUERIES ======
    
    /**
     * Find consumption records by device/sensor
     */
    List<WaterConsumption> findByDeviceId(Long deviceId);

}
