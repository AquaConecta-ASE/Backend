
package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record CreateResidentResource(
        String firstName,
        String lastName,
        String email,
        String direction,
        String documentNumber,
        String documentType,
        String phone,
        Float waterTankSize
) {}
