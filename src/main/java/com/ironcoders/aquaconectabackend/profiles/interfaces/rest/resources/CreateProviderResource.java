
package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record CreateProviderResource(
        String taxName,
        String ruc,
        String firstName,
        String lastName,
        String email,
        String direction,
        String documentNumber,
        String documentType,
        String phone
) {}
