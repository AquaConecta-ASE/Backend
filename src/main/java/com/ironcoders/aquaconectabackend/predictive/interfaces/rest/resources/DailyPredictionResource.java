package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources;

public record DailyPredictionResource(
    String date,
    Double predictedConsumption,
    String dayOfWeek
) {}