package com.ironcoders.aquaconectabackend.profiles.domain.model.commands;

import java.util.Objects;

public record UpdateProviderCommand (String taxName, String ruc, Long userId ){

        public UpdateProviderCommand{
            Objects.requireNonNull(taxName, "taxName cannot be null");
            Objects.requireNonNull(ruc, "ruc cannot be null");
            Objects.requireNonNull(userId, "userId cannot be null");

        }

}
