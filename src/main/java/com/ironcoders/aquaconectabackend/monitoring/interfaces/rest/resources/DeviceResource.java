package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources;

public record DeviceResource(
        Long id,
        String type,
        String status,
        Long residentId
){
}
