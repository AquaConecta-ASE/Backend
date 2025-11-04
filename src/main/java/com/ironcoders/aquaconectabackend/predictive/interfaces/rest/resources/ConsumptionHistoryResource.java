package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources;

public record ConsumptionHistoryResource(
    Long id,
    Long residentId,
    String date,
    Double consumption,
    Long deviceId,
    String waterQuality,
    Double initialLevel,
    Double finalLevel
) {}