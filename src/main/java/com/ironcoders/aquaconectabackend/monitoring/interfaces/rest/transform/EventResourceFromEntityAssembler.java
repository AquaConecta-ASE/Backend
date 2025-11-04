package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.CreateEventResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.EventResource;

public class EventResourceFromEntityAssembler {
    public static EventResource toResourceFromEntity(Event entity) {
        return new EventResource(
                entity.getId(),
                entity.getEventType(),
                entity.getQualityValue(),
                entity.getLevelValue(),
                entity.getSensorId()

        );
    }
}