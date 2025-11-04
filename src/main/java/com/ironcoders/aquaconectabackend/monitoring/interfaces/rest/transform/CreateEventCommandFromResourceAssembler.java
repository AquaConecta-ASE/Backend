package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.CreateEventResource;

public class CreateEventCommandFromResourceAssembler {
    public static CreateEventCommand toCommandFromResource(CreateEventResource resource) {
        return new CreateEventCommand(
                resource.eventType(),
                resource.qualityValue(),
                resource.levelValue(),
                resource.sensorId()
        );
    }


}