package com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProviderByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderQueryService;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProvidersQuery;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class ProviderContextFacade {

    private final ProviderQueryService providerQueryService;

    public ProviderContextFacade(ProviderQueryService providerQueryService) {
        this.providerQueryService = providerQueryService;
    }

    /**
     * Fetches a provider by userId using a Query object.
     */
    public Optional<Provider> fetchProviderByUserId(Long userId) {
        return providerQueryService.handle(new GetProviderByUserIdQuery(userId));
    }

    /**
     * Fetches a provider by userId directly (if needed).
     */
    public Optional<Provider> fetchProviderByUserIdDirect(long userId) {
        return providerQueryService.findByUserId(userId);
    }

    /**
     * Fetches all providers.
     */
    public List<Provider> fetchAllProviders() {
        return providerQueryService.handle(new GetAllProvidersQuery());
    }
}