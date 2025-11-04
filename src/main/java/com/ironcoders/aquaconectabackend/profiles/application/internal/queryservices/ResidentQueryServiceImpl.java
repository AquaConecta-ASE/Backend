package com.ironcoders.aquaconectabackend.profiles.application.internal.queryservices;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllResidentsQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentsByProviderIdQuery;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResidentQueryServiceImpl implements ResidentQueryService {

    private final ResidentRepository residentRepository;

    public ResidentQueryServiceImpl(ResidentRepository residentRepository) {
        this.residentRepository = residentRepository;
    }


    @Override
    public Optional<Resident> handle(GetResidentByUserIdQuery query) {

       return  residentRepository.findByUserId(query.userId())
                .stream()
                .findFirst();
    }

    @Override
    public List<Resident> handle(GetResidentsByProviderIdQuery query) {
        return residentRepository.findByProviderId(query.providerId());
    }

    @Override
    public List<Resident> findByUserId(Long userId) {
        return residentRepository.findByUserId(userId);
    }


    @Override
    public List<Resident> handle(GetAllResidentsQuery query) {
        return residentRepository.findAll();
    }


    @Override
    public Optional<Resident> findById(Long residentId) {
        return residentRepository.findById(residentId);
    }
}
