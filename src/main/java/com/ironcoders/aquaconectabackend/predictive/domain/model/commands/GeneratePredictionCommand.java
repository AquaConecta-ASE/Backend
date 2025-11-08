package com.ironcoders.aquaconectabackend.predictive.domain.model.commands;

public record GeneratePredictionCommand(
        Long subscriptionId,
        Long residentId
) {
    public void validate() {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new IllegalArgumentException("Subscription ID must be a positive number.");
        }
        if (residentId == null || residentId <= 0) {
            throw new IllegalArgumentException("Resident ID must be a positive number.");
        }
    }
}
