package com.ironcoders.aquaconectabackend.monitoring.domain.services;

import java.util.List;
import java.util.Optional;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByIdQuery;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByResidentId;

public interface DeviceQueryService {

    Optional<Device> handle(GetDeviceByResidentId query);
    List<Device> handle(GetAllDevicesByResidentId query);


    Optional<Device>handle(GetDeviceByIdQuery query);

}
