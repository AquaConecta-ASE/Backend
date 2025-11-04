package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllResidentsQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentsByProviderIdQuery;

import java.util.List;
import java.util.Optional;

public interface ResidentQueryService{
    Optional<Resident> handle(GetResidentByUserIdQuery query);
    List<Resident> handle(GetResidentsByProviderIdQuery query);
    List<Resident> findByUserId(Long userId);
    Optional<Resident> findById(Long residentId);
    List<Resident> handle(GetAllResidentsQuery query);
}
