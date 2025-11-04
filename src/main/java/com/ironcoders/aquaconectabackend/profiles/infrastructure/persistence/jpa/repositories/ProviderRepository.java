package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    List<Provider> findByUserId(Long userId);
}