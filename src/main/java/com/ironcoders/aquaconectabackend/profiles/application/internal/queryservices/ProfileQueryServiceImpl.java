package com.ironcoders.aquaconectabackend.profiles.application.internal.queryservices;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProfilesQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {
    private final ProfileRepository profileRepository;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> handle(GetProfileByIdQuery query) {
        return profileRepository.findById(query.id());
    }

    @Override
    public Optional<Profile> handle(GetProfileByUserIdQuery query) {
        return profileRepository.findByUserId(query.userId())
                .stream()
                .findFirst();
    }

    @Override
    public List<Profile> handle(GetAllProfilesQuery query) {
        return profileRepository.findAll();
    }
}
