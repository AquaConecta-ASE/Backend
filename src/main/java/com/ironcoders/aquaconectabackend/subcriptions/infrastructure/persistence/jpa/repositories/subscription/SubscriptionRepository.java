package com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByResidentId(Long residentId);

    List<Subscription> findAllByResidentId(Long residentId);
    List<Subscription> findByProviderId(Long providerId);

    long countByStatus(String status);

    @Query("SELECT COUNT(s) * 258.0f FROM Subscription s")
    Float sumAllPrices();

    @Query("SELECT COUNT(s) * 258.0f FROM Subscription s " +
            "WHERE MONTH(s.startDate) = MONTH(CURRENT_DATE) AND YEAR(s.startDate) = YEAR(CURRENT_DATE)")
    Float sumCurrentMonthPrices();
}
