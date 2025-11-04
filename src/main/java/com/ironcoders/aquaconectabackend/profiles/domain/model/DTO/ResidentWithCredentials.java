package com.ironcoders.aquaconectabackend.profiles.domain.model.dto;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;

public record ResidentWithCredentials(
        Resident resident,
        String username,
        String password
) {
}
