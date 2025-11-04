package com.ironcoders.aquaconectabackend.profiles.application.internal.queryservices;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProvidersQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProviderByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProviderQueryServiceImpl implements ProviderQueryService {
    private final ProviderRepository providerRepository;

    public ProviderQueryServiceImpl(ProviderRepository profileRepository) {
        this.providerRepository = profileRepository;
    }


    @Override
    public Optional<Provider> handle(GetProviderByUserIdQuery query) {
        List<Provider> providers = providerRepository.findByUserId(query.userId());
        if (providers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(providers.get(0));
    }

    @Override
    public Optional<Provider> findByUserId(long userId) {
        List<Provider> providers = providerRepository.findByUserId(userId);
        if (providers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(providers.get(0));
    }

    @Override
    public List<Provider> handle(GetAllProvidersQuery query) {
        List<Provider>providers = providerRepository.findAll();
        return providers;
    }

}
