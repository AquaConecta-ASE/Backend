package com.ironcoders.aquaconectabackend.predictive.domain.model.commands;

public record GeneratePredictionCommand(
        Long residentId
) {
    public void validate() {
        if (residentId == null || residentId <= 0) {
            throw new IllegalArgumentException("Resident ID must be a positive number.");
        }
    }
}
