package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.DeviceResource;

public class DeviceResourceFromEntityAssembler {
    public static DeviceResource toResourceFromEntity(Device aggregate) {
        return new DeviceResource(
            aggregate.getId(),
                aggregate.getStatus(),
                aggregate.getDescription(),
                aggregate.getResidentId()
        );
    }
}