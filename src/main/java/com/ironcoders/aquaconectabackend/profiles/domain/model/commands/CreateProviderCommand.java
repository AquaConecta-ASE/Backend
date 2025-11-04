package com.ironcoders.aquaconectabackend.profiles.domain.model.commands;

import java.util.Objects;
public record CreateProviderCommand(
        String taxName,
        String ruc,
        String firstName,
        String lastName,
        String email,
        String direction,
        String documentNumber,
        String documentType,
        String phone,
        Long userId
) {
    public CreateProviderCommand {
        Objects.requireNonNull(taxName);
        Objects.requireNonNull(ruc);
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(email);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(documentNumber);
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(phone);
        Objects.requireNonNull(userId, "userId cannot be null");
    }
}


