package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MLPredictionResponse {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("dailyAverageConsumption")
    private Double dailyAverageConsumption;

    @JsonProperty("next7DaysPredictions")
    private List<DailyPredictionDTO> next7DaysPredictions;

    @JsonProperty("waterRunoutDate")
    private String waterRunoutDate; // formato: "2025-11-15"

    @JsonProperty("daysUntilRunout")
    private Integer daysUntilRunout;

    @JsonProperty("confidenceScore")
    private Double confidenceScore;

    @JsonProperty("currentWaterLevel")
    private Double currentWaterLevel;

    @JsonProperty("totalPredictedConsumption7Days")
    private Double totalPredictedConsumption7Days;
}
