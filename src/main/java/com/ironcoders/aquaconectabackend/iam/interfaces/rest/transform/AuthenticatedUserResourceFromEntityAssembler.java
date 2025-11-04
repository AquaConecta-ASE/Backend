package com.ironcoders.aquaconectabackend.iam.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntity(User user, String token) {
        return new AuthenticatedUserResource(user.getId(), user.getUsername(), token);
    }
}
