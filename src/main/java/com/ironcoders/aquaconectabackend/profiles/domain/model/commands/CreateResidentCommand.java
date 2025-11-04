package com.ironcoders.aquaconectabackend.profiles.domain.model.commands;

import java.util.Objects;

public record CreateResidentCommand(
        String firstName,
        String lastName,
        String email,
        String direction,
        String documentNumber,
        String documentType,
        String phone,
        Long providerId,
        Float waterTankSize
) {
    public CreateResidentCommand {
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(email);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(documentNumber);
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(phone);
        Objects.requireNonNull(providerId, "providerId cannot be null");
    }
}
