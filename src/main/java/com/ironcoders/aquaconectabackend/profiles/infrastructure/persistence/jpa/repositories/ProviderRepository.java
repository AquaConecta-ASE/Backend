package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    List<Provider> findByUserId(Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Provider p SET p.ruc = :ruc WHERE p.id = :id")
    void updateRuc(@Param("id") Long id, @Param("ruc") String ruc);
}