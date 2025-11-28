package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProviderResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ResidentResource;

public class ResidentResourceFromEntityAssembler {

    // Caso básico sin credenciales
    public static ResidentResource toResourceFromEntity(Resident entity, Profile profile) {
        // Handle null profile (resident hasn't logged in yet)
        String phone = profile != null ? profile.getPhone() : null;
        String direction = profile != null ? profile.getDirection() : null;
        String documentNumber = profile != null ? profile.getDocumentNumber() : null;
        
        return new ResidentResource(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                phone,
                direction,
                documentNumber,
                entity.getProviderId(),
                entity.getUserId(),
                null,     // username vacío
                null      // password vacío
        );
    }

    // Caso con credenciales generadas (crear)
    public static ResidentResource toResourceFromEntityWithCredentials(
            Resident entity,
            String username,
            String password,
            Profile profile
    ) {
        return new ResidentResource(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                profile.getPhone(),
                profile.getDirection(),
                profile.getDocumentNumber(),
                entity.getProviderId(),
                entity.getUserId(),
                username,
                password
        );
    }
}
