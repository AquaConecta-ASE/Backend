package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record ProviderResource(
        Long id,
        String taxName,
        String ruc,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String direction,
        String documentNumber,
        String documentType,
        String phone
) {}
