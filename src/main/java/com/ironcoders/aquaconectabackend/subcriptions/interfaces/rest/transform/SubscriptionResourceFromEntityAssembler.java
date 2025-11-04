package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources.SubscriptionResource;

public class SubscriptionResourceFromEntityAssembler {

    public static SubscriptionResource toResourceFromEntity(Subscription entity) {
        return new SubscriptionResource(entity.getId(), entity.getStartDate().toString(),
                entity.getEndDate().toString(), entity.getStatus(), entity.getSensorId(),
                entity.getResidentId(), entity.getWaterTankSize());
    }
}
