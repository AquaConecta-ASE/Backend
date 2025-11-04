package com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands;

import java.util.Objects;

public record CreateAdditionalSubscriptionCommand(Long residentId, Float waterTankSize) {

        public CreateAdditionalSubscriptionCommand{
        Objects.requireNonNull(residentId);
        Objects.requireNonNull(waterTankSize);
    }
}