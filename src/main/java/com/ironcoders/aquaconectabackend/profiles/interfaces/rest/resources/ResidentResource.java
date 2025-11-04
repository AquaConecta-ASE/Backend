package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record ResidentResource(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String address,
        String documentNumber,
        Long providerId,
        Long userId,
        String username,
        String password
) {}
