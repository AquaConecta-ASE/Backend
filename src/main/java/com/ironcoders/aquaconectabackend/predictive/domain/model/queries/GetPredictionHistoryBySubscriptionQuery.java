package com.ironcoders.aquaconectabackend.predictive.domain.model.queries;

/**
 * Query to get all prediction history for a subscription.
 * Returns all predictions regardless of status (ACTIVE, OUTDATED).
 * Useful for providers to see prediction history.
 */
public record GetPredictionHistoryBySubscriptionQuery(Long subscriptionId) {
    
    public GetPredictionHistoryBySubscriptionQuery {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new IllegalArgumentException("Subscription ID must be positive");
        }
    }
}
