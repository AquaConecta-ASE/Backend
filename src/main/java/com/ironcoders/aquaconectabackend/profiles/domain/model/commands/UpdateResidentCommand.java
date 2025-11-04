package com.ironcoders.aquaconectabackend.profiles.domain.model.commands;

import java.util.Objects;

public record UpdateResidentCommand(String firstName, String lastName, Long userId) {
    public UpdateResidentCommand {
        Objects.requireNonNull(firstName, "firstName cannot be null");
        Objects.requireNonNull(lastName, "lastName cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
    }

}
