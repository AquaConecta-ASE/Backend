package com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateAdditionalSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.UpdateSubscriptionCommand;

import java.util.Optional;

public interface SubscriptionCommandService {
    Optional<Subscription> handle (CreateSubscriptionCommand command);
    Optional<Subscription>handle(UpdateSubscriptionCommand command);
    Optional<Subscription>handle(CreateAdditionalSubscriptionCommand command);
}
