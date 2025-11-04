package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProvidersQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProviderByUserIdQuery;

import java.util.List;
import java.util.Optional;

public interface ProviderQueryService {
    Optional<Provider> handle(GetProviderByUserIdQuery query);

    Optional<Provider> findByUserId(long userId);

    List<Provider> handle(GetAllProvidersQuery query);
}
