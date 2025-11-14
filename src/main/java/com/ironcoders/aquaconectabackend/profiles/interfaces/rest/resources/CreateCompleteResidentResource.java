package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources;

import jakarta.validation.constraints.*;

/**
 * Resource for creating a complete resident ecosystem.
 * The provider ID is automatically extracted from the authenticated user.
 * The device serial number is automatically generated.
 */
public record CreateCompleteResidentResource(
    @NotBlank(message = "First name is required")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Phone is required")
    String phone,
    
    @NotBlank(message = "Address is required")
    String direction,
    
    @NotBlank(message = "Document type is required")
    String documentType,
    
    @NotBlank(message = "Document number is required")
    String documentNumber,
    
    @NotNull(message = "Water tank size is required")
    @Positive(message = "Water tank size must be positive")
    Integer waterTankSize
) {
    // providerId is NOT in the request - obtained from authenticated provider
    // deviceSerialNumber is NOT in the request - generated automatically
    // planId is NOT in the request - not needed for subscription
}

