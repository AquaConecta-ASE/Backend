package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateAdditionalSubscriptionCommand;

public class CreateSubscriptionCommandFromResourceAssembler {

    public static CreateAdditionalSubscriptionCommand toCommandFromResource( CreateAdditionalSubscriptionCommand resource) {
        return new CreateAdditionalSubscriptionCommand(
                resource.residentId()
                , resource.waterTankSize()
        );
    }

}
