package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources;

import java.util.List;

public record PredictionResponseResource(
    Long residentId,
    String predictionDate,
    Double dailyAverageConsumption,
    List<DailyPredictionResource> next7DaysPredictions,
    String waterRunoutDate,
    Integer daysUntilRunout,
    Double confidenceScore,
    Double currentWaterLevel,
    Double totalPredictedConsumption7Days,
    String status
) {}
