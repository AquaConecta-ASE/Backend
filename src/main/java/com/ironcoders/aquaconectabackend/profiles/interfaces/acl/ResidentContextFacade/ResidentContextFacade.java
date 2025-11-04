package com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentQueryService;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllResidentsQuery;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResidentContextFacade {

    private final ResidentQueryService residentQueryService;

    public ResidentContextFacade(ResidentQueryService residentQueryService) {
        this.residentQueryService = residentQueryService;
    }

    /**
     * Fetches a resident by their userId.
     *
     * @param userId the user id.
     * @return an Optional with the resident, or empty if not found.
     */
    public Optional<Resident> fetchResidentByUserId(Long userId) {
        // Usa el método handle con el query adecuado
        return residentQueryService.handle(new GetResidentByUserIdQuery(userId));
    }

    /**
     * Fetches a list of residents by their userId.
     *
     * @param userId the user id.
     * @return a list of residents.
     */
    public List<Resident> fetchResidentsByUserId(Long userId) {
        return residentQueryService.findByUserId(userId);
    }

    /**
     * Fetches all residents.
     *
     * @return a list of all residents.
     */
    public List<Resident> fetchAllResidents() {
        return residentQueryService.handle(new GetAllResidentsQuery());
    }

    public List<Resident> findByUserId(Long userId) {
        return residentQueryService.findByUserId(userId);
    }

    public Optional<Resident> findById(Long residentId) {
        return residentQueryService.findById(residentId);
    }


}