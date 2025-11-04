package com.ironcoders.aquaconectabackend.profiles.interfaces.rest;


import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileCommandService;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileQueryService;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.CreateProfileCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.UpdateProfileCommandFromResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    /**
     * Constructor for dependency injection.
     * @param profileCommandService Service for profile commands
     * @param profileQueryService Service for profile queries
     */
    public ProfilesController(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
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
        // 1. Get the userId of the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // 2. Pass it to the assembler to create the command
        CreateProfileCommand createProfileCommand = CreateProfileCommandFromResourceAssembler.toCommandFromResource(resource, userId);

        // 3. Call the service as usual
        var profile = profileCommandService.handle(createProfileCommand);
        if (profile.isEmpty()) return ResponseEntity.badRequest().build();
        var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
        return new ResponseEntity<>(profileResource, HttpStatus.CREATED);
    }

    /**
     * Endpoint to retrieve the profile of the authenticated user.
     * Accessible by ADMIN, PROVIDER, or RESIDENT roles.
     * @return ResponseEntity with the profile resource or NOT_FOUND if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<ProfileResource> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        var getProfileByIdQuery = new GetProfileByUserIdQuery(userDetails.getId());
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        long userId = userDetails.getId();

        UpdateProfileCommand updateProfileCommand = UpdateProfileCommandFromResource.toCommandFromResource(resource, userId);

        Optional<Profile> updatedProfileOptional = profileCommandService.handle(updateProfileCommand);

        return updatedProfileOptional
                .map(updatedProfile -> ResponseEntity.ok(ProfileResourceFromEntityAssembler.toResourceFromEntity(updatedProfile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
