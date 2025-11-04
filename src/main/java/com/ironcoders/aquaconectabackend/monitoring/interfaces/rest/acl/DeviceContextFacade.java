package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl;

import org.springframework.stereotype.Service;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceCommandService;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceQueryService;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceContextFacade {

    private final DeviceQueryService deviceQueryService;
    private final DeviceCommandService deviceCommandService;
    
    public DeviceContextFacade(DeviceQueryService deviceQueryService, DeviceCommandService deviceCommandService) {
        this.deviceQueryService = deviceQueryService;
        this.deviceCommandService = deviceCommandService;
    }

    /**
     * Fetches all devices by residentId.
     *
     * @param residentId the resident id
     * @return a list of Device
     */


    /**
     * Creates a new device for a resident.
     *
     * @param device the information to create the device
     * @return the created Device
     */
    public Optional<Device> createDevice(CreateDeviceCommand command) {
        return deviceCommandService.handle(command);
    }

    public List<Device> getAllDevicesByResidentId(Long residentId) {
        return deviceQueryService.handle(new GetAllDevicesByResidentId(residentId));
    }
}