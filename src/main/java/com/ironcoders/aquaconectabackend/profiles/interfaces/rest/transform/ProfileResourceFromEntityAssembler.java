package com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProfileResource;

public class ProfileResourceFromEntityAssembler {
    public static ProfileResource toResourceFromEntity(Profile entity) {
        return new ProfileResource(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getDirection(), entity.getDocumentNumber(), entity.getDocumentType(), entity.getPhone(),entity.getUserId());

    }


}
