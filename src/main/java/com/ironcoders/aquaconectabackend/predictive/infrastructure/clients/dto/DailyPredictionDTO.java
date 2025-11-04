package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DailyPredictionDTO {

    @JsonProperty("date")
    private String date; // formato: "2025-10-28"

    @JsonProperty("predictedConsumption")
    private Double predictedConsumption;

    @JsonProperty("dayOfWeek")
    private String dayOfWeek; // "Monday", "Tuesday", etc.
}