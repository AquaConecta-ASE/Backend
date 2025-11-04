package com.ironcoders.aquaconectabackend.monitoring.domain.services;

import java.util.Optional;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.UpdateEventCommand;

public interface EventCommandService {
    Optional<Event> handle(CreateEventCommand command);

    Optional<Event> handle(UpdateEventCommand command);

}