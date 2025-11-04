package com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByResidentId(Long residentId);
    List<Device> findAllByResidentId(Long residentId);
}
