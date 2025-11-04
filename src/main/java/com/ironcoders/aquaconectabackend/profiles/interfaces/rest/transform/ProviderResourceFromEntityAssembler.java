package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProviderResource;
public class ProviderResourceFromEntityAssembler {

    public static ProviderResource toResourceFromEntities(Provider provider, Profile profile) {
        return new ProviderResource(
                provider.getId(),
                provider.getTaxName(),
                provider.getRuc(),
                provider.getUserId(),
                profile.getName().getFirstName(),
                profile.getName().getLastName(),
                profile.getEmail(),
                profile.getDirection(),
                profile.getDocumentNumber(),
                profile.getDocumentType(),
                profile.getPhone()
        );
    }
}

