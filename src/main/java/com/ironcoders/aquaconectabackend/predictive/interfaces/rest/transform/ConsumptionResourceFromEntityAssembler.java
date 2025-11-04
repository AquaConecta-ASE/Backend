package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.ConsumptionHistoryResource;

public class ConsumptionResourceFromEntityAssembler {

    /**
     * Transforms a WaterConsumption entity to a REST resource.
     */
    public static ConsumptionHistoryResource toResourceFromEntity(WaterConsumption entity) {
        return new ConsumptionHistoryResource(
            entity.getId(),
            entity.getResidentId(),
            entity.getDate().toString(),
            entity.getConsumption(),
            entity.getDeviceId(),
            entity.getWaterQuality(),
            entity.getInitialLevel(),
            entity.getFinalLevel()
        );
    }
}