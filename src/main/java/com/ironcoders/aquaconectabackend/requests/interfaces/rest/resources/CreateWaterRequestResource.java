package com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources;

import java.time.LocalDateTime;

public record CreateWaterRequestResource(
        Long residentId,
        Long providerId,
        String requestedLiters,
        String status,
        LocalDateTime deliveredAt) {
}
