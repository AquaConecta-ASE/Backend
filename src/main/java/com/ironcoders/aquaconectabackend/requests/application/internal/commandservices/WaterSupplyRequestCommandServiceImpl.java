package com.ironcoders.aquaconectabackend.requests.application.internal.commandservices;

import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.services.WaterSupplyRequestCommandService;
import com.ironcoders.aquaconectabackend.requests.infrastructure.persistence.jpa.repositories.WaterSupplyRequestRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Service

public class WaterSupplyRequestCommandServiceImpl implements WaterSupplyRequestCommandService {

    private final WaterSupplyRequestRepository waterRequestRepository;
    private final ResidentContextFacade residentContextFacade;
    private final ProviderContextFacade providerContextFacade;

    public WaterSupplyRequestCommandServiceImpl(WaterSupplyRequestRepository waterRequestRepository, ResidentContextFacade residentContextFacade, ProviderContextFacade providerContextFacade) {    
        this.waterRequestRepository = waterRequestRepository;
        this.residentContextFacade = residentContextFacade;
        this.providerContextFacade = providerContextFacade;
    }

    @Override
    public Optional<WaterSupplyRequest> handle(CreateWaterSupplyRequestCommand command) throws AccessDeniedException {
        // Usar el ResidentContextFacade para obtener el residente por userId
        List<Resident> residents = residentContextFacade.findByUserId(command.userId());
        if (residents.isEmpty()) {
            throw new IllegalArgumentException("Residente no encontrado.");
        }

        Resident resident = residents.get(0);

        WaterSupplyRequest waterRequest = new WaterSupplyRequest(
                resident.getId(),
                resident.getProviderId(),
                command.requestedLiters(),
                command.status(),
                null
        );

        waterRequestRepository.save(waterRequest);
        return Optional.of(waterRequest);
    }



    @Override
    public Optional<WaterSupplyRequest> handle(UpdateWaterSupplyRequestCommand command) throws AccessDeniedException {
        // 1. Obtener el proveedor usando el ProviderContextFacade y el userId del comando
        Optional<Provider> providers = providerContextFacade.fetchProviderByUserIdDirect(command.userId());
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("Proveedor no encontrado.");
        }
        Provider provider = providers.get();

        // 2. Buscar la solicitud de agua
        WaterSupplyRequest waterRequest = waterRequestRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada."));

        // 3. Verificar que la solicitud pertenezca a este proveedor
        if (!waterRequest.getProviderId().equals(provider.getId())) {
            throw new AccessDeniedException("No tienes permiso para actualizar esta solicitud.");
        }

        // 4. Actualizar estado y fecha de entrega
        waterRequest.update(command.status(), command.deliveredAt());

        // 5. Guardar cambios
        waterRequestRepository.save(waterRequest);

        return Optional.of(waterRequest);
    }

}