package com.ironcoders.aquaconectabackend.profiles.interfaces.rest;


import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileCommandService;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.CreateProfileCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.UpdateProfileCommandFromResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for profile management endpoints.
 * Provides endpoints to create, retrieve, and update user profiles.
 */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Profile Management Endpoints")
@PreAuthorize("isAuthenticated()")
public class ProfilesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilesController.class);
    
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;

    /**
     * Constructor for dependency injection.
     * @param profileCommandService Service for profile commands
     * @param profileQueryService Service for profile queries
     * @param userRepository Repository for user queries
     * @param providerRepository Repository for provider queries
     */
    public ProfilesController(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService, 
                            UserRepository userRepository, ProviderRepository providerRepository) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
    }

    /**
     * Endpoint to create a new profile.
     * Only accessible by ADMIN or PROVIDER roles.
     * @param resource The request body containing profile data
     * @return ResponseEntity with the created profile resource or BAD_REQUEST if creation fails
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER')")
    public ResponseEntity<ProfileResource> createProfile(@RequestBody CreateProfileResource resource) {
        // 1. Get the auth0Id from the JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        // 2. Find user in local DB by auth0Id
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = userOptional.get().getId();

        // 3. Pass it to the assembler to create the command
        CreateProfileCommand createProfileCommand = CreateProfileCommandFromResourceAssembler.toCommandFromResource(resource, userId);

        // 4. Call the service as usual
        var profile = profileCommandService.handle(createProfileCommand);
        if (profile.isEmpty()) return ResponseEntity.badRequest().build();
        
        // 5. Si el usuario es PROVIDER, actualizar el RUC con el documentNumber del Profile
        updateProviderRucIfNeeded(userId, profile.get());
        
        var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
        return new ResponseEntity<>(profileResource, HttpStatus.CREATED);
    }
    
    /**
     * Actualiza el RUC del Provider con el documentNumber del Profile si existe un Provider para este usuario
     */
    private void updateProviderRucIfNeeded(Long userId, Profile profile) {
        try {
            List<Provider> providers = providerRepository.findByUserId(userId);
            if (!providers.isEmpty()) {
                Provider provider = providers.get(0);
                String documentNumber = profile.getDocumentNumber();
                
                // Solo actualizar si el RUC está pendiente o vacío
                if (provider.getRuc() == null || provider.getRuc().equals("PENDIENTE") || provider.getRuc().trim().isEmpty()) {
                    LOGGER.info("📝 Actualizando RUC del Provider ID {} con documentNumber: {}", provider.getId(), documentNumber);
                    
                    // Usar una query nativa para actualizar el RUC
                    providerRepository.updateRuc(provider.getId(), documentNumber);
                    
                    LOGGER.info("✅ RUC del Provider actualizado exitosamente");
                } else {
                    LOGGER.info("ℹ️ RUC del Provider ya está configurado: {}", provider.getRuc());
                }
            }
        } catch (Exception e) {
            LOGGER.error("❌ Error actualizando RUC del Provider: {}", e.getMessage(), e);
            // No fallar el createProfile si esto falla
        }
    }

    /**
     * Endpoint to retrieve the profile of the authenticated user.
     * Accessible by ADMIN, PROVIDER, or RESIDENT roles.
     * @return ResponseEntity with the profile resource or NOT_FOUND if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<ProfileResource> getMyProfile() {
        // Get the auth0Id from the JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        // Find user in local DB by auth0Id
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = userOptional.get().getId();

        var getProfileByIdQuery = new GetProfileByUserIdQuery(userId);
        var profile = profileQueryService.handle(getProfileByIdQuery);
        if (profile.isEmpty()) return ResponseEntity.notFound().build();
        var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
        return ResponseEntity.ok(profileResource);
    }

    /**
     * Endpoint to update the profile of the authenticated user.
     * Accessible by ADMIN, PROVIDER, or RESIDENT roles.
     * @param resource The request body containing updated profile data
     * @return ResponseEntity with the updated profile resource or NOT_FOUND if not found
     */
    @PutMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<ProfileResource> updateProfile(@RequestBody UpdateProfileResource resource) {
        // Get the auth0Id from the JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        // Find user in local DB by auth0Id
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = userOptional.get().getId();

        UpdateProfileCommand updateProfileCommand = UpdateProfileCommandFromResource.toCommandFromResource(resource, userId);

        Optional<Profile> updatedProfileOptional = profileCommandService.handle(updateProfileCommand);

        return updatedProfileOptional
                .map(updatedProfile -> ResponseEntity.ok(ProfileResourceFromEntityAssembler.toResourceFromEntity(updatedProfile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
