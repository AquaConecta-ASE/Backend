package com.ironcoders.aquaconectabackend.monitoring.domain.model.commads;

public record CreateEventCommand(
        String eventType,
        String qualityValue,
        String levelValue,
        Long sensorId
) {


}