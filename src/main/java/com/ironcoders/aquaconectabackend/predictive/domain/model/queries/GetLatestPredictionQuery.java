package com.ironcoders.aquaconectabackend.predictive.domain.model.queries;

/**
 * Query to get the latest valid prediction for a resident
 */
public record GetLatestPredictionQuery(Long residentId) {
}
