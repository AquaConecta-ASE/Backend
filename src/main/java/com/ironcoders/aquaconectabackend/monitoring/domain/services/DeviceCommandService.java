package com.ironcoders.aquaconectabackend.monitoring.domain.services;

import java.util.Optional;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;

public interface DeviceCommandService {
    Optional<Device> handle(CreateDeviceCommand command);

}
