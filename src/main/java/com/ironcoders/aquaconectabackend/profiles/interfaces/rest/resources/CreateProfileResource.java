package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record CreateProfileResource(
        String firstName, String lastName, String email, String direction, String documentNumber, String documentType, String phone) {

}
