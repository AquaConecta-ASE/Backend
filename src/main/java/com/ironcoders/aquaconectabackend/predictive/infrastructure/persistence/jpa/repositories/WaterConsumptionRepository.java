package com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterConsumptionRepository extends JpaRepository<WaterConsumption, Long> {

    List<WaterConsumption> findByResidentId(Long residentId);

    List<WaterConsumption> findByResidentIdOrderByDateAsc(Long residentId);

    List<WaterConsumption> findByResidentIdAndDateBetweenOrderByDateAsc(
            Long residentId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<WaterConsumption> findByResidentIdAndDate(Long residentId, LocalDate date);

    boolean existsByResidentIdAndDate(Long residentId, LocalDate date);

    List<WaterConsumption> findByDeviceId(Long deviceId);

    List<WaterConsumption> findByResidentIdAndDateBetweenAndIsRefillFalseOrderByDateAsc(
            Long residentId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find only refill days for a resident
     */
    List<WaterConsumption> findByResidentIdAndIsRefillTrueOrderByDateDesc(Long residentId);

    /**
     * Count refills in a date range
     */
    Long countByResidentIdAndDateBetweenAndIsRefillTrue(
            Long residentId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Find the most recent refill day
     */
    Optional<WaterConsumption> findFirstByResidentIdAndIsRefillTrueOrderByDateDesc(Long residentId);

}
