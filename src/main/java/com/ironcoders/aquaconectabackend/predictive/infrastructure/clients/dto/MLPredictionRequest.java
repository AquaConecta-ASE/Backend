package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MLPredictionRequest {
    

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("historicalData")
    private List<HistoricalDataPoint> historicalData;

    @JsonProperty("currentWaterLevel")
    private Double currentWaterLevel;

    /**
     * Nested class representing a historical data point.
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HistoricalDataPoint {
        
        @JsonProperty("date")
        private String date; // formato: "2025-10-27"
        
        @JsonProperty("consumption")
        private Double consumption; // litros consumidos
    }
}