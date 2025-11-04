package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateProviderResource;

public class UpdateProviderCommandFromResource {
    public static UpdateProviderCommand toCommandFromResource(UpdateProviderResource resource, Long userId) {
        return new UpdateProviderCommand(resource.taxName(), resource.ruc(), userId);
    }
}
