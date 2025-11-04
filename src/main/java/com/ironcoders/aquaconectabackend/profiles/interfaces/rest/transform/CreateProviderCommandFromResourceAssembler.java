package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateProviderResource;
public class CreateProviderCommandFromResourceAssembler {
    public static CreateProviderCommand toCommandFromResource(CreateProviderResource resource, Long userId) {
        return new CreateProviderCommand(
                resource.taxName(),
                resource.ruc(),
                resource.firstName(),
                resource.lastName(),
                resource.email(),
                resource.direction(),
                resource.documentNumber(),
                resource.documentType(),
                resource.phone(),
                userId
        );
    }
}
