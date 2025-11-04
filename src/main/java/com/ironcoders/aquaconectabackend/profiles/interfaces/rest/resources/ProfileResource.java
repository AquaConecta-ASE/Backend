package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record ProfileResource(
        Long id,
        String firstName, String lastName, String email, String direction, String documentNumber, String documentType,String phone,
        Long userId) { }
