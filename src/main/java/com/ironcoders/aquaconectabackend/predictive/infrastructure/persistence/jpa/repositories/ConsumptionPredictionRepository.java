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

    List<ConsumptionPrediction> findByResidentIdOrderByPredictionDateDesc(Long residentId);

    Optional<ConsumptionPrediction> findFirstByResidentIdOrderByPredictionDateDesc(Long residentId);

    Optional<ConsumptionPrediction> findFirstByResidentIdAndStatusOrderByPredictionDateDesc(
            Long residentId,
            PredictionStatus status
    );

    List<ConsumptionPrediction> findByStatusAndPredictionDateBefore(
            PredictionStatus status,
            LocalDateTime date
    );

        /**
 * Find predictions by resident and status
 */
List<ConsumptionPrediction> findByResidentIdAndStatus(Long residentId, PredictionStatus status);

    boolean existsByResidentIdAndStatus(Long residentId, PredictionStatus status);

}
