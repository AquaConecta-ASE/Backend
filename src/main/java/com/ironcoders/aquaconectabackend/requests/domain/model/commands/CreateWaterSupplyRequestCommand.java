package com.ironcoders.aquaconectabackend.requests.domain.model.commands;

import java.time.LocalDateTime;

public record CreateWaterSupplyRequestCommand(
        Long residentId,
        Long providerId,
        String requestedLiters,
        String status,
        LocalDateTime deliveredAt,
        Long userId     
) {}