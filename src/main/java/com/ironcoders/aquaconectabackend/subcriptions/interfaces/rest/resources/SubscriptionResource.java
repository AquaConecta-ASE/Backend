package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources;

public record SubscriptionResource(
        Long id,
        String startDate,
        String endDate,
        String status,
        Long sensorId, Long residentId, Float waterTankSize) {
}
