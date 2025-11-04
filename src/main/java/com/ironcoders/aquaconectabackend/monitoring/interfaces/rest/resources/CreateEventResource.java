package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources;

public record CreateEventResource( String eventType,
        String qualityValue,
        String levelValue,
        Long sensorId
) {}