package com.ironcoders.aquaconectabackend.monitoring.domain.model.commads;

public record UpdateEventCommand(
        Long eventId,
        String eventType,
        String qualityValue,
        String levelValue,
        Long sensorId
) {}