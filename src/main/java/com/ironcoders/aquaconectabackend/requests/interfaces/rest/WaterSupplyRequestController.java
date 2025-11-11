package com.ironcoders.aquaconectabackend.requests.interfaces.rest;

import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetAllWaterSupplyRequestsQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetWaterSupplyRequestByIdQuery;
import com.ironcoders.aquaconectabackend.requests.domain.services.WaterSupplyRequestCommandService;
import com.ironcoders.aquaconectabackend.requests.domain.services.WaterSupplyRequestQueryService;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.CreateWaterRequestResource;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.UpdateWaterResource;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.WaterSupplyRequestResource;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform.CreateWaterSupplyRequestCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform.UpdateWaterSupplyRequestCommandFromResource;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform.WaterRequestResourceFromAggregateAssembler;

import io.jsonwebtoken.lang.Collections;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/water-supply-requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Water Supply Requests", description = "Water Supply Request Management endpoints")
@PreAuthorize("isAuthenticated()")
public class WaterSupplyRequestController {

    private final WaterSupplyRequestCommandService waterRequestCommandService;
    private final WaterSupplyRequestQueryService waterRequestQueryService;
    private final ResidentContextFacade residentContextFacade;
    private final ProviderContextFacade providerContextFacade;
    private final UserRepository userRepository;

    public WaterSupplyRequestController(WaterSupplyRequestCommandService waterRequestCommandService, WaterSupplyRequestQueryService waterRequestQueryService, ResidentContextFacade residentContextFacade, ProviderContextFacade providerContextFacade, UserRepository userRepository) {
        this.waterRequestCommandService = waterRequestCommandService;
        this.waterRequestQueryService = waterRequestQueryService;
        this.residentContextFacade = residentContextFacade;
        this.providerContextFacade = providerContextFacade;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_RESIDENT') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public List<WaterSupplyRequestResource> getAllMyWaterRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = userOptional.get().getId();

        boolean isResident = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        boolean isProvider = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<WaterSupplyRequest> requests = waterRequestQueryService.handle(new GetAllWaterSupplyRequestsQuery());

        if (isAdmin) {
            // El admin puede ver todo
            return requests.stream()
                    .map(WaterRequestResourceFromAggregateAssembler::toResourceFromEntity)
                    .collect(Collectors.toList());
        }

        if (isResident) {
            Optional<Resident> residentOptional = residentContextFacade.fetchResidentByUserId(userId);
            if (residentOptional.isEmpty()) {
                return Collections.emptyList();
            }
            Long residentId = residentOptional.get().getId();
            return requests.stream()
                    .filter(request -> request.getResidentId().equals(residentId))
                    .map(WaterRequestResourceFromAggregateAssembler::toResourceFromEntity)
                    .collect(Collectors.toList());
        }

        if (isProvider) {
            Optional<Provider> providerOptional = providerContextFacade.fetchProviderByUserId(userId);
            if (providerOptional.isEmpty()) {
                return Collections.emptyList();
            }
            Long providerId = providerOptional.get().getId();
            return requests.stream()
                    .filter(request -> request.getProviderId().equals(providerId))
                    .map(WaterRequestResourceFromAggregateAssembler::toResourceFromEntity)
                    .collect(Collectors.toList());
        }

        // Si no es ninguno de los anteriores, retorna vacío
        return Collections.emptyList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<WaterSupplyRequestResource> getWaterRequestById(@PathVariable Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOptional.get().getId();

        Optional<WaterSupplyRequest> requestOptional = waterRequestQueryService.handle(new GetWaterSupplyRequestByIdQuery(id));
        if (requestOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        WaterSupplyRequest request = requestOptional.get();

        boolean isResident = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        if (isResident) {
            Optional<Resident> residentOptional = residentContextFacade.fetchResidentByUserId(userId);
            if (residentOptional.isEmpty() || !residentOptional.get().getId().equals(request.getResidentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        boolean isProvider = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"));
        if (isProvider) {
            Optional<Provider> providerOptional = providerContextFacade.fetchProviderByUserId(userId);
            if (providerOptional.isEmpty() || !providerOptional.get().getId().equals(request.getProviderId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        WaterSupplyRequestResource resource = WaterRequestResourceFromAggregateAssembler.toResourceFromEntity(request);
        return ResponseEntity.ok(resource);
    }


    @PostMapping
    @PreAuthorize("hasRole('ROLE_RESIDENT')")
    public ResponseEntity<WaterSupplyRequestResource> createWaterRequest(@RequestBody CreateWaterRequestResource resource) throws AccessDeniedException {
        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOptional.get().getId();


        CreateWaterSupplyRequestCommand command = CreateWaterSupplyRequestCommandFromResourceAssembler.toCommandFromResource(resource, userId);
        var waterRequestAggregate = waterRequestCommandService.handle(command);
        if (waterRequestAggregate.isEmpty())  return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        var waterResource = WaterRequestResourceFromAggregateAssembler.toResourceFromEntity(waterRequestAggregate.get());
        return new ResponseEntity<>(waterResource, HttpStatus.CREATED);

    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PROVIDER')")
    public ResponseEntity<WaterSupplyRequestResource> updateWaterRequest(
            @PathVariable Long id,
            @RequestBody UpdateWaterResource resource) throws AccessDeniedException {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String auth0Id = authentication.getName();
            
            var userOptional = userRepository.findByAuth0Id(auth0Id);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Long userId = userOptional.get().getId();

        UpdateWaterSupplyRequestCommand command = UpdateWaterSupplyRequestCommandFromResource.toCommandFromResource(id, resource, userId);

        Optional<?> result = waterRequestCommandService.handle(command);
        return result
                .map(obj -> WaterRequestResourceFromAggregateAssembler.toResourceFromEntity((com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest) obj))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

     
    }


}