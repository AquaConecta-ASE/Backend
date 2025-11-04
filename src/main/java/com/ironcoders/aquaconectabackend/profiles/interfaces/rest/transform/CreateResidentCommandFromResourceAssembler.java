package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateResidentResource;

public class CreateResidentCommandFromResourceAssembler {
    public static CreateResidentCommand toCommandFromResource(CreateResidentResource resource, Long providerId) {
        return new CreateResidentCommand(
                resource.firstName(),
                resource.lastName(),
                resource.email(),
                resource.direction(),
                resource.documentNumber(),
                resource.documentType(),
                resource.phone(),
                providerId,
                resource.waterTankSize()
        );
    }
}