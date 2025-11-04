package com.ironcoders.aquaconectabackend.monitoring.application.internal.commandservices;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceCommandService;
import com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories.DeviceRepository;

/**
 * Service implementation for device command operations.
 * Handles creation and persistence of Device entities.
 */
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository deviceRepository;

    /**
     * Constructor for dependency injection.
     * @param deviceRepository Repository for Device entities
     */
    public DeviceCommandServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Handles the creation of a new Device based on the provided command.
     * @param command The command containing device creation data
     * @return An Optional containing the created Device
     */
    @Override
    public Optional<Device> handle(CreateDeviceCommand command) {
        // Create a new Device entity from the command
        var device = new Device(command);
        // Save the device to the repository
        deviceRepository.save(device);
        // Return the created device wrapped in an Optional
        return Optional.of(device);
    }
}
