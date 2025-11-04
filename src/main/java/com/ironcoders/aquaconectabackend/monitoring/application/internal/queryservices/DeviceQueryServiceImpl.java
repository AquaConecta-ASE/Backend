package com.ironcoders.aquaconectabackend.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByIdQuery;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceQueryService;
import com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories.DeviceRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

    private final DeviceRepository sensorRepository;

    public DeviceQueryServiceImpl(DeviceRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Override
    public Optional<Device> handle(GetDeviceByResidentId query) {
        return sensorRepository.findByResidentId(query.residentId());
    }

    @Override
    public List<Device> handle(GetAllDevicesByResidentId query) {
        return sensorRepository.findAllByResidentId(query.residentId());
    }


    @Override
    public Optional<Device>handle(GetDeviceByIdQuery query){
        return sensorRepository.findById(query.sensorId());
    }
}
