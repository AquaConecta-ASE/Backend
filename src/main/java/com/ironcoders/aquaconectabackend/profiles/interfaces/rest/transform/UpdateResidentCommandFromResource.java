package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateResidentResource;

public class UpdateResidentCommandFromResource {
    public static UpdateResidentCommand toCommandFromResource(UpdateResidentResource resource, Long userId) {
        return new UpdateResidentCommand(resource.firstName(), resource.lastName(), userId);


    }
}
