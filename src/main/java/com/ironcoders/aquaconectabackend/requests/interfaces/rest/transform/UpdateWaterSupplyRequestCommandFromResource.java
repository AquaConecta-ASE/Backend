package com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.UpdateWaterResource;

public class UpdateWaterSupplyRequestCommandFromResource {

    public static UpdateWaterSupplyRequestCommand toCommandFromResource(Long id, UpdateWaterResource resource, Long userId) {
        return new UpdateWaterSupplyRequestCommand(id,resource.status(),resource.deliveredAt(), userId);
    }
}
