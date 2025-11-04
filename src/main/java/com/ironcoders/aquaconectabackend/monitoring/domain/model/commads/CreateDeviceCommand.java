package com.ironcoders.aquaconectabackend.monitoring.domain.model.commads;

public record CreateDeviceCommand(
        String type,
        String status,
        String description,
        Long residentId
) {
   
}