package com.ironcoders.aquaconectabackend.profiles.domain.services;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProfilesQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByUserIdQuery;

import java.util.List;
import java.util.Optional;

public interface ProfileQueryService {
    Optional<Profile> handle(GetProfileByIdQuery query);
    List<Profile> handle(GetAllProfilesQuery query);
    Optional<Profile> handle(GetProfileByUserIdQuery query);
    
}
