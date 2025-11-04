package com.ironcoders.aquaconectabackend.predictive.domain.model.queries;
/**
 * Query to get the latest prediction for a resident
 */
public record GetPredictionByResidentIdQuery(Long residentId) {
}
