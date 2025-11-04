package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.UpdateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources.UpdateSubscriptionResource;

public class UpdateSubscriptionCommandFromResource {
    public static UpdateSubscriptionCommand toCommand(Long id, UpdateSubscriptionResource resource) {
        return new UpdateSubscriptionCommand(
                id,
                resource.endDate(),
                resource.status()
        );
    }

}
