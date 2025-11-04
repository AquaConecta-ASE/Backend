package com.ironcoders.aquaconectabackend.predictive.domain.model.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DailyPrediction {

    private LocalDate date;
    private Double predictedConsumption;
    private String dayOfWeek;

    /**
     * Check if this prediction is for a weekend
     */
    public boolean isWeekend() {
        int dayValue = date.getDayOfWeek().getValue();
        return dayValue == 6 || dayValue == 7; // Saturday or Sunday
    }
}
