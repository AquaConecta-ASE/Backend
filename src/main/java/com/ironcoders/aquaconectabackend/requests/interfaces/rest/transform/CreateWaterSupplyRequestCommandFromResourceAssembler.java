package com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.CreateWaterRequestResource;

public class CreateWaterSupplyRequestCommandFromResourceAssembler {
    public static CreateWaterSupplyRequestCommand toCommandFromResource(CreateWaterRequestResource resource, Long userId) {
        return new CreateWaterSupplyRequestCommand(
                resource.residentId(),
                resource.providerId(),
                resource.requestedLiters(),
                resource.status(),
                resource.deliveredAt(),
                userId
        );
    }

}