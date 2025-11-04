package com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.WaterSupplyRequestResource;

public class WaterRequestResourceFromAggregateAssembler {
    public static WaterSupplyRequestResource toResourceFromEntity(WaterSupplyRequest aggregate) {
        return new WaterSupplyRequestResource(
                aggregate.getId(),
                aggregate.getResidentId(),
                aggregate.getProviderId(),
                aggregate.getRequestedLiters(),
                aggregate.getEmissionDate(),
                aggregate.getStatus(),
                aggregate.getDeliveredAt()
        );
    }
}
