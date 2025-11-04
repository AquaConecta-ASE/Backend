package com.ironcoders.aquaconectabackend.profiles.domain.model.commands;

import java.util.Objects;

public record UpdateProfileCommand(String firstName, String lastName, String email, String direction, String documentNumber, String documentType, String phone, Long userId ) {

        public UpdateProfileCommand {
            Objects.requireNonNull(firstName, "firstName cannot be null");
            Objects.requireNonNull(lastName, "lastName cannot be null");
            Objects.requireNonNull(email, "email cannot be null");
            Objects.requireNonNull(direction, "direction cannot be null");
            Objects.requireNonNull(documentNumber, "documentNumber cannot be null");
            Objects.requireNonNull(documentType, "documentType cannot be null");
            Objects.requireNonNull(phone, "phone cannot be null");
            Objects.requireNonNull(userId, "userId cannot be null");
        }
}
