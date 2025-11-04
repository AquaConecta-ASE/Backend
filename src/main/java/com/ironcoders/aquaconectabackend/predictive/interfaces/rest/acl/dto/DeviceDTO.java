package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceDTO {

    private Long id;
    private String type;
    private String status;
    private String description;
    private Long residentId;

    /**
     * Checks if the device is active.
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}