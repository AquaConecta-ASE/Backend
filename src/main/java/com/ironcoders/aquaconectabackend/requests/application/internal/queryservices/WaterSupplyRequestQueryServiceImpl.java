package com.ironcoders.aquaconectabackend.requests.application.internal.queryservices;
import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetWaterRequestsByResidentIdQuery;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetAllWaterSupplyRequestsByProviderQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetAllWaterSupplyRequestsQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetWaterSupplyRequestByIdQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetWaterSupplyRequestsByResidentIdQuery;
import com.ironcoders.aquaconectabackend.requests.domain.services.WaterSupplyRequestQueryService;
import com.ironcoders.aquaconectabackend.requests.infrastructure.persistence.jpa.repositories.WaterSupplyRequestRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WaterSupplyRequestQueryServiceImpl implements WaterSupplyRequestQueryService {

    private final WaterSupplyRequestRepository waterRequestRepository;
    private final ProviderContextFacade providerContextFacade;

    public WaterSupplyRequestQueryServiceImpl(WaterSupplyRequestRepository waterRequestRepository, ProviderContextFacade providerContextFacade) {
        this.waterRequestRepository = waterRequestRepository;
        this.providerContextFacade = providerContextFacade;
    }

    @Override
    public Optional<WaterSupplyRequest> handle(GetWaterSupplyRequestByIdQuery query) {
        return waterRequestRepository.findById(query.waterRequestId());
    }

    public List<WaterSupplyRequest> handle(GetAllWaterSupplyRequestsByProviderQuery query) {
        // Obtener el providerId usando el ProviderContextFacade
        Long providerId = providerContextFacade.fetchProviderByUserId(query.userId())
                .map(Provider::getId)
                .orElse(null);

        if (providerId == null) {
            throw new IllegalArgumentException("Proveedor no encontrado.");
        }

        // Filtrar las solicitudes por providerId
        List<WaterSupplyRequest> allRequests = waterRequestRepository.findAll();
        return allRequests.stream()
                .filter(request -> request.getProviderId().equals(providerId))
                .toList();
    }


  
    @Override
    public List<WaterSupplyRequest> handle(GetAllWaterSupplyRequestsQuery query) {
        return waterRequestRepository.findAll();
    }

    @Override
    public List<WaterSupplyRequest> handle(GetWaterRequestsByResidentIdQuery getWaterRequestsByResidentIdQuery) {
 
        Long residentId = getWaterRequestsByResidentIdQuery.residentId();
        return waterRequestRepository.findByResidentId(residentId);

    }

 
}