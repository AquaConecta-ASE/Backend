package com.ironcoders.aquaconectabackend.iam.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SignInCommand;
import com.ironcoders.aquaconectabackend.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(signInResource.username(), signInResource.password());
    }
}
