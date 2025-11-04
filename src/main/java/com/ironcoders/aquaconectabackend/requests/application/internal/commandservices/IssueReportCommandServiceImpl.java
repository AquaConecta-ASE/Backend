package com.ironcoders.aquaconectabackend.requests.application.internal.commandservices;
import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.IssueReport;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateIssueReportCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateIssueReportCommand;
import com.ironcoders.aquaconectabackend.requests.domain.services.IssueReportCommandService;
import com.ironcoders.aquaconectabackend.requests.infrastructure.persistence.jpa.repositories.IssueReportRepository;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class IssueReportCommandServiceImpl implements IssueReportCommandService {

    private final IssueReportRepository requestRepository;
    private final ResidentContextFacade residentContextFacade;
    private final ProviderContextFacade providerContextFacade;

    public IssueReportCommandServiceImpl(
            IssueReportRepository requestRepository,
            ResidentContextFacade residentContextFacade,
            ProviderContextFacade providerContextFacade) {
        this.requestRepository = requestRepository;
        this.residentContextFacade = residentContextFacade;
        this.providerContextFacade = providerContextFacade;
    }

    @Override
    public Optional<IssueReport> handle(CreateIssueReportCommand command) {
        long userId = command.userId();

        var residentOpt = residentContextFacade.fetchResidentByUserId(userId);
        if (residentOpt.isEmpty()) {
            throw new IllegalArgumentException("Residente no encontrado.");
        }

        Resident resident = residentOpt.get();

        IssueReport requestAggregate = new IssueReport(
                resident.getId(),
                resident.getProviderId(),
                command.title(),
                command.description(),
                command.status()
        );

        requestRepository.save(requestAggregate);
        return Optional.of(requestAggregate);
    }

    @Override
    public Optional<IssueReport> handle(UpdateIssueReportCommand command) throws AccessDeniedException {
        long userId = command.userId();

        // Buscar el proveedor usando el ProviderContextFacade
        Optional<Provider> providerOpt = providerContextFacade.fetchProviderByUserId(userId);
        if (providerOpt.isEmpty()) {
            throw new IllegalArgumentException("No se encontró un proveedor asociado al usuario con ID: " + userId);
        }
        Provider provider = providerOpt.get();

        // Buscar el IssueReport
        IssueReport requestAggregate = requestRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con ID: " + command.id()));

        // Verificar que la solicitud pertenezca a este proveedor
        if (!requestAggregate.getProviderId().equals(provider.getId())) {
            throw new AccessDeniedException("No tienes permiso para actualizar esta solicitud.");
        }

        // Actualizar el estado de la solicitud
        requestAggregate.update(command.status());

        // Guardar cambios
        requestRepository.save(requestAggregate);

        return Optional.of(requestAggregate);
    }


}