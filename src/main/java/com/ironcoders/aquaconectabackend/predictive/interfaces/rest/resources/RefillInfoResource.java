package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources;

public record RefillInfoResource(
    Integer refillsLast30Days,
    String lastRefillDate,
    Integer daysSinceLastRefill
) {
    
}
