package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;

import java.util.List;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long> {
    List<Resident> findByUserId(Long userId);
    List<Resident> findByProviderId(Long providerId);

}
