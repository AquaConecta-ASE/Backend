package com.ironcoders.aquaconectabackend.iam.domain.model.commands;


import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;

import java.util.List;

/**
 * SignUpCommand with optional Auth0 user creation support.
 * 
 * @param username Username for local authentication
 * @param password Password (hashed) for local authentication
 * @param roles List of roles to assign
 * @param email Optional: Email for Auth0 user creation
 * @param firstName Optional: First name for Auth0 user creation
 * @param lastName Optional: Last name for Auth0 user creation
 * @param providerId Optional: Provider ID to store in Auth0 app_metadata
 */
public record SignUpCommand(
    String username, 
    String password, 
    List<Role> roles,
    String email,
    String firstName,
    String lastName,
    Long providerId
) {
    // Constructor for backward compatibility (without Auth0 fields)
    public SignUpCommand(String username, String password, List<Role> roles) {
        this(username, password, roles, null, null, null, null);
    }
}

