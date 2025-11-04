package com.ironcoders.aquaconectabackend.predictive.domain.model.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ConsumptionPeriod {

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Creates a period for the last N days
     */
    public static ConsumptionPeriod lastDays(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return new ConsumptionPeriod(startDate, endDate);
    }

    /**
     * Creates a period for the last month
     */
    public static ConsumptionPeriod lastMonth() {
        return lastDays(30);
    }

    /**
     * Validates that start date is before end date
     */
    public boolean isValid() {
        return startDate.isBefore(endDate) || startDate.isEqual(endDate);
    }

    /**
     * Gets the number of days in this period
     */
    public long getDaysCount() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
