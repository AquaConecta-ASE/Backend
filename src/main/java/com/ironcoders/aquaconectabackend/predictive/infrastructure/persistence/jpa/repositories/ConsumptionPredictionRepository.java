package com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction.PredictionStatus;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionPredictionRepository extends JpaRepository<ConsumptionPrediction, Long> {

    // ====== PRIMARY QUERIES (by subscriptionId) ======
    
    /**
     * Find latest prediction for a subscription (PRIMARY)
     */
    Optional<ConsumptionPrediction> findFirstBySubscriptionIdAndStatusOrderByPredictionDateDesc(
            Long subscriptionId,
            PredictionStatus status
    );

    /**
     * Find all predictions for a subscription ordered by date
     */
    List<ConsumptionPrediction> findBySubscriptionIdOrderByPredictionDateDesc(Long subscriptionId);

    /**
     * Find predictions by subscription and status
     */
    List<ConsumptionPrediction> findBySubscriptionIdAndStatus(Long subscriptionId, PredictionStatus status);

    /**
     * Check if a subscription has any predictions with given status
     */
    boolean existsBySubscriptionIdAndStatus(Long subscriptionId, PredictionStatus status);

    // ====== SECONDARY QUERIES (by residentId - for getting all predictions of a resident) ======
    
    /**
     * Find all predictions for a resident (across all their subscriptions)
     */
    List<ConsumptionPrediction> findByResidentIdOrderByPredictionDateDesc(Long residentId);

    /**
     * Find latest prediction for a resident (DEPRECATED - use subscription-based query)
     */
    @Deprecated
    Optional<ConsumptionPrediction> findFirstByResidentIdOrderByPredictionDateDesc(Long residentId);

    /**
     * Find active predictions for a resident (across all subscriptions)
     */
    List<ConsumptionPrediction> findByResidentIdAndStatusOrderByPredictionDateDesc(
            Long residentId,
            PredictionStatus status
    );

    /**
     * Find predictions by status before a certain date (for cleanup/archiving)
     */
    List<ConsumptionPrediction> findByStatusAndPredictionDateBefore(
            PredictionStatus status,
            LocalDateTime date
    );

}
