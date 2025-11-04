package com.ironcoders.aquaconectabackend.profiles.interfaces.rest;


import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.iam.interfaces.acl.IamContextFacade;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProvidersQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProviderByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentsByProviderIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProviderCommandService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentQueryService;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateProviderResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProviderResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateProviderResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.CreateProviderCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.ProviderResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.ResidentResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.UpdateProviderCommandFromResource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for provider management endpoints.
 * Provides endpoints to create, update, and retrieve providers and their related data.
 */
@RestController
@RequestMapping(value = "/api/v1/providers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Providers", description = "Provider Management Endpoints")
@PreAuthorize("isAuthenticated()")
public class ProviderController {
    private final ProviderCommandService providerCommandService;
    private final ProviderQueryService providerQueryService;
    private final ProfileRepository profileRepository;
    private final ProviderRepository providerRepository;
    private final ResidentQueryService residentQueryService;
    IamContextFacade iamContextFacade;

    /**
     * Constructor for dependency injection.
     * @param providerCommandService Service for provider commands
     * @param providerQueryService Service for provider queries
     * @param profileRepository Repository for profiles
     * @param providerRepository Repository for providers
     * @param residentQueryService Service for resident queries
     * @param iamContextFacade IAM context facade for user info
     */
    public ProviderController(ProviderCommandService providerCommandService, ProviderQueryService providerQueryService, ProfileRepository profileRepository, ProviderRepository providerRepository , ResidentQueryService residentQueryService, IamContextFacade iamContextFacade) {
        this.providerCommandService = providerCommandService;
        this.providerQueryService = providerQueryService;
        this.profileRepository = profileRepository;
        this.providerRepository = providerRepository;
        this.residentQueryService = residentQueryService;
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Endpoint to create a new provider profile.
     * Only accessible by PROVIDER or ADMIN roles.
     * @param resource The request body containing provider data
     * @return ResponseEntity with the created provider resource or error message
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createProfile(@RequestBody CreateProviderResource resource) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        long userId = userDetails.getId();

        // ✅ Validar si ya existe un perfil para el usuario autenticado
        List<Profile> profiles = profileRepository.findByUserId(userId);
        if (!profiles.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Ya existe un perfil asociado a esta cuenta.");
        }

        // Crear proveedor
        CreateProviderCommand createProviderCommand = CreateProviderCommandFromResourceAssembler.toCommandFromResource(resource, userId);
        var providerOptional = providerCommandService.handle(createProviderCommand);

        if (providerOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Error al crear proveedor.");
        }

        var provider = providerOptional.get();

        // Buscar perfil nuevamente después de creación
        List<Profile> updatedProfiles = profileRepository.findByUserId(userId);
        if (updatedProfiles.isEmpty()) {
            return ResponseEntity.internalServerError().body("Perfil no encontrado tras la creación.");
        }

        var providerResource = ProviderResourceFromEntityAssembler.toResourceFromEntities(provider, updatedProfiles.get(0));
        return new ResponseEntity<>(providerResource, HttpStatus.CREATED);
    }

    /**
     * Endpoint to update a provider's profile.
     * Only accessible by PROVIDER or ADMIN roles.
     * @param resource The request body containing updated provider data
     * @return ResponseEntity with the updated provider resource or NOT_FOUND if not found
     */
    @PutMapping("/{providerId}/profiles")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderResource> updateProvider(@RequestBody UpdateProviderResource resource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        long userId = userDetails.getId();

        UpdateProviderCommand updateProviderCommand = UpdateProviderCommandFromResource.toCommandFromResource(resource, userId);
        Optional<Provider> updatedProviderOptional = providerCommandService.handle(updateProviderCommand);

        if (updatedProviderOptional.isEmpty() || updatedProviderOptional.get().getUserId() != userId) {
            return ResponseEntity.notFound().build();
        }

        Provider updatedProvider = updatedProviderOptional.get();

        // Obtener el perfil actualizado
        List<Profile> profileOptional = profileRepository.findByUserId(userId);
        if (profileOptional.isEmpty()) {
            return ResponseEntity.internalServerError().build(); // No debería pasar
        }

        ProviderResource response = ProviderResourceFromEntityAssembler.toResourceFromEntities(updatedProvider, profileOptional.get(0));
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to get all residents associated with a provider.
     * Only accessible by PROVIDER or ADMIN roles.
     * @param providerId The ID of the provider
     * @return ResponseEntity with a list of resident resources
     */
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{providerId}/residents")
    public ResponseEntity<List<ResidentResource>> getResidentsByProviderId(@PathVariable("providerId") Long providerId) {
     
        var query = new GetResidentsByProviderIdQuery(providerId);
        var residents = residentQueryService.handle(query);
        if (residents.isEmpty()) return ResponseEntity.notFound().build();

        var residentResources = residents.stream().map(resident -> {


            List<Profile> profiles = profileRepository.findByUserId(resident.getUserId());
            String username = iamContextFacade.fetchUsernameByUserId(resident.getUserId());
            return ResidentResourceFromEntityAssembler.toResourceFromEntityWithCredentials(resident, username,null, profiles.get(0));
        }).toList();

        return ResponseEntity.ok(residentResources);
    }

    /**
     * Endpoint to get a provider by its ID.
     * Only accessible by ADMIN role.
     * @param providerId The ID of the provider
     * @return ResponseEntity with the provider resource or NOT_FOUND if not found
     */
    @GetMapping(value = "/{providerId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProviderResource> getProviderById(@PathVariable Long providerId) {
        var providerOptional = providerRepository.findById(providerId);

        if (providerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var provider = providerOptional.get();

        var profileOptional = profileRepository.findByUserId(provider.getUserId());
        if (profileOptional.isEmpty()) {
            return ResponseEntity.internalServerError().build();
        }

        var resource = ProviderResourceFromEntityAssembler.toResourceFromEntities(provider, profileOptional.get(0));
        return ResponseEntity.ok(resource);
    }

    /**
     * Endpoint to get the provider details for the authenticated provider.
     * Only accessible by PROVIDER role.
     * @return ResponseEntity with the provider resource or NOT_FOUND if not found
     */
    @GetMapping("/{providerId}/profiles")
    @PreAuthorize("hasRole('ROLE_PROVIDER')")
    public ResponseEntity<ProviderResource> getMyProviderDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Obtener el proveedor a partir del userId
        var query = new GetProviderByUserIdQuery(userId);
        var providerOptional = providerQueryService.handle(query);

        if (providerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var provider = providerOptional.get();

        // Obtener el perfil asociado
        var profileOptional = profileRepository.findByUserId(provider.getUserId());
        if (profileOptional.isEmpty()) {
            return ResponseEntity.internalServerError().build(); // No debería ocurrir
        }

        var resource = ProviderResourceFromEntityAssembler.toResourceFromEntities(provider, profileOptional.get(0));
        return ResponseEntity.ok(resource);
    }

    /**
     * Endpoint to get all providers.
     * Only accessible by ADMIN role.
     * @return ResponseEntity with a list of provider resources
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ProviderResource>> getAllProviders() {
        List<Provider> providers = providerQueryService.handle(new GetAllProvidersQuery());

        if (providers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<ProviderResource> resources = new ArrayList<>();

        for (Provider provider : providers) {
            var profileOptional = profileRepository.findByUserId(provider.getUserId());
            if (profileOptional.isEmpty()) {
                continue; // Podrías loguear esto si es un caso raro
            }
            var resource = ProviderResourceFromEntityAssembler.toResourceFromEntities(
                    provider, profileOptional.get(0)
            );
            resources.add(resource);
        }

        return ResponseEntity.ok(resources);
    }



}
