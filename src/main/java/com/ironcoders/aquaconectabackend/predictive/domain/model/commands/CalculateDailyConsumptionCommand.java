package com.ironcoders.aquaconectabackend.predictive.domain.model.commands;

import java.time.LocalDate;

public record CalculateDailyConsumptionCommand(
        Long subscriptionId,
        Long residentId,
        LocalDate startDate,
        LocalDate endDate
) {
    public void validate() {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new IllegalArgumentException("Subscription ID must be positive");
        }
        if (residentId == null || residentId <= 0) {
            throw new IllegalArgumentException("Resident ID must be positive");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
}
