package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

public record CompleteResidentResource(
    Long residentId,
    String auth0UserId,
    Long profileId,
    Long subscriptionId,
    Long deviceId,
    String email,
    String fullName,
    Integer waterTankSize,
    String message,
    String passwordResetUrl  // URL for password setup
) {
    public CompleteResidentResource(
        Long residentId,
        String auth0UserId,
        Long profileId,
        Long subscriptionId,
        Long deviceId,
        String email,
        String firstName,
        String lastName,
        Integer waterTankSize,
        String passwordResetUrl
    ) {
        this(
            residentId,
            auth0UserId,
            profileId,
            subscriptionId,
            deviceId,
            email,
            firstName + " " + lastName,
            waterTankSize,
            "Resident created successfully. Password reset email sent.",
            passwordResetUrl
        );
    }
}
