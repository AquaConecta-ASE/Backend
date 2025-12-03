package com.ironcoders.aquaconectabackend.profiles.interfaces.rest;

// import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.iam.interfaces.acl.IamContextFacade;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl.DeviceContextFacade;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.DeviceResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform.DeviceResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.dto.ResidentWithCredentials;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllResidentsQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetAllProfilesQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetResidentsByProviderIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetWaterRequestsByResidentIdQuery;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileQueryService;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ResidentCommandService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.ResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.UpdateResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CreateCompleteResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.resources.CompleteResidentResource;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.CreateResidentCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.ResidentResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.profiles.interfaces.rest.transform.UpdateResidentCommandFromResource;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.IssueReport;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.acl.IssueReportContextFacade;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.acl.WaterSupplyRequestContextFacade;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.WaterSupplyRequestResource;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform.WaterRequestResourceFromAggregateAssembler;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptionsByResidentId;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.acl.SubscriptionContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources.SubscriptionResource;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.shared.Infrastructure.auth0.Auth0ManagementService;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceCommandService;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileCommandService;
import com.ironcoders.aquaconectabackend.profiles.domain.model.valueobjects.PersonName;
import com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription.SubscriptionCommandService;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.auth0.exception.Auth0Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.auth0.exception.Auth0Exception;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.List;

/**
 * REST controller for resident management endpoints.
 * Provides endpoints to create, update, and retrieve residents and their related data.
 */
@RestController
@RequestMapping(value = "/api/v1/residents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Residents", description = "Resident Management Endpoints")
@PreAuthorize("isAuthenticated()")
public class ResidentController {

    private static final Logger logger = LoggerFactory.getLogger(ResidentController.class);

    private final ResidentCommandService residentCommandService;
    private final ResidentQueryService residentQueryService;
    private final ResidentRepository residentRepository;
    private final ProviderQueryService providerQueryService;
    private final ProfileQueryService profileQueryService;
    private final ProfileCommandService profileCommandService;
    private final UserRepository userRepository;
    private final Auth0ManagementService auth0ManagementService;
    private final DeviceCommandService deviceCommandService;
    private final SubscriptionCommandService subscriptionCommandService;

    IamContextFacade iamContextFacade;
    WaterSupplyRequestContextFacade waterSupplyRequestContextFacade;
    IssueReportContextFacade issueReportContextFacade;
    DeviceContextFacade  deviceContextFacade;
    SubscriptionContextFacade subscriptionContextFacade;

    /**
     * Constructor for dependency injection.
     * @param residentCommandService Service for resident commands
     * @param residentQueryService Service for resident queries
     * @param residentRepository Repository for residents
     * @param providerQueryService Service for provider queries
     * @param profileRepository Repository for profiles
     * @param iamContextFacade IAM context facade for user info
     * @param waterSupplyRequestContextFacade Facade for water supply requests
     * @param profileQueryService Service for profile queries
     * @param profileCommandService Service for profile commands
     * @param issueReportContextFacade Facade for issue reports
     * @param deviceContextFacade Facade for device context
     * @param subscriptionContextFacade Facade for subscription context
     * @param userRepository Repository for user queries
     * @param auth0ManagementService Service for Auth0 Management API
     * @param deviceCommandService Service for device commands
     * @param subscriptionCommandService Service for subscription commands
     */
    public ResidentController(
            ResidentCommandService residentCommandService,
            ResidentQueryService residentQueryService,
            ResidentRepository residentRepository,
            ProviderQueryService providerQueryService,
            ProfileRepository profileRepository,
            IamContextFacade iamContextFacade,
            WaterSupplyRequestContextFacade waterSupplyRequestContextFacade,
            ProfileQueryService profileQueryService,
            ProfileCommandService profileCommandService,
            IssueReportContextFacade issueReportContextFacade,
            DeviceContextFacade deviceContextFacade,
            SubscriptionContextFacade subscriptionContextFacade,
            UserRepository userRepository,
            Auth0ManagementService auth0ManagementService,
            DeviceCommandService deviceCommandService,
            SubscriptionCommandService subscriptionCommandService
    ) {
        this.residentCommandService = residentCommandService;
        this.residentQueryService = residentQueryService;
        this.residentRepository = residentRepository;
        this.providerQueryService = providerQueryService;
        this.iamContextFacade = iamContextFacade;
        this.waterSupplyRequestContextFacade = waterSupplyRequestContextFacade;
        this.profileQueryService = profileQueryService;
        this.profileCommandService = profileCommandService;
        this.issueReportContextFacade = issueReportContextFacade;
        this.deviceContextFacade = deviceContextFacade;
        this.subscriptionContextFacade = subscriptionContextFacade;
        this.userRepository = userRepository;
        this.auth0ManagementService = auth0ManagementService;
        this.deviceCommandService = deviceCommandService;
        this.subscriptionCommandService = subscriptionCommandService;
    }

    /**
     * Endpoint to create a new resident with complete ecosystem:
     * - Creates Auth0 user account
     * - Creates Resident record
     * - Creates Profile with all data
     * - Creates Subscription with waterTankSize
     * - Auto-generates and assigns Device with unique serial number
     * 
     * Only accessible by PROVIDER role.
     * Provider ID is automatically extracted from the authenticated user.
     * Device serial number is auto-generated with format: IOT-{providerId}-{timestamp}
     * All operations are atomic - if any step fails, everything is rolled back.
     * 
     * @param resource The request body containing complete resident data
     * @return ResponseEntity with created entity IDs and status
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('ROLE_PROVIDER')")
    @Transactional
    public ResponseEntity<CompleteResidentResource> createCompleteResident(
            @RequestBody CreateCompleteResidentResource resource) {
        
        // Get authenticated provider
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOptional.get().getId();
        
        // Verify provider exists
        Optional<Provider> providerOptional = providerQueryService.findByUserId(userId);
        if (providerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CompleteResidentResource(
                            null, null, null, null, null, 
                            resource.email(), 
                            resource.firstName() + " " + resource.lastName(),
                            resource.waterTankSize(),
                            "Provider not found",
                            null));  // No password reset URL
        }
        
        Long providerId = providerOptional.get().getId();
        
        String auth0UserId = null;
        Resident resident = null;
        Profile profile = null;
        Subscription subscription = null;
        Device device = null;
        
        try {
            // Step 1: Create user in Auth0
            logger.info("🔄 Step 1: Creating user in Auth0...");
            auth0UserId = auth0ManagementService.createResidentUser(
                    resource.email(),
                    resource.firstName(),
                    resource.lastName(),
                    providerId
            );
            logger.info("✅ Step 1 completed: Auth0 user ID: {}", auth0UserId);
            
            // Step 2: Create Resident in local DB (without userId yet)
            logger.info("🔄 Step 2: Creating Resident in local DB...");
            resident = new Resident(resource.firstName(), resource.lastName(), null, providerId);
            resident = residentRepository.save(resident);
            logger.info("✅ Step 2 completed: Resident ID: {}", resident.getId());
            
            // Step 3: Create Profile
            logger.info("🔄 Step 3: Creating Profile...");
            CreateProfileCommand profileCommand = new CreateProfileCommand(
                    resource.firstName(),
                    resource.lastName(),
                    resource.email(),
                    resource.direction(),
                    resource.documentNumber(),
                    resource.documentType(),
                    resource.phone(),
                    null // userId will be set on first login by interceptor
            );
            try {
                Optional<Profile> profileOptional = profileCommandService.handle(profileCommand);
                if (profileOptional.isEmpty()) {
                    throw new RuntimeException("Failed to create profile");
                }
                profile = profileOptional.get();
                logger.info("✅ Step 3 completed: Profile ID: {}", profile.getId());
            } catch (Exception e) {
                logger.error("❌ Step 3 FAILED: {}", e.getMessage(), e);
                throw e;
            }
            
            // Step 4: Create Device with auto-generated serial number
            logger.info("🔄 Step 4: Creating Device...");
            // Format: IOT-{providerId}-{timestamp}
            String autoGeneratedSerial = String.format("IOT-%d-%d", 
                    providerId, 
                    System.currentTimeMillis());
            
            CreateDeviceCommand deviceCommand = new CreateDeviceCommand(
                    autoGeneratedSerial,
                    "ACTIVE",
                    "TDS/HC-SR04",
                    resident.getId()
            );
            Optional<Device> deviceOptional = deviceCommandService.handle(deviceCommand);
            if (deviceOptional.isEmpty()) {
                throw new RuntimeException("Failed to create device");
            }
            device = deviceOptional.get();
            logger.info("✅ Step 4 completed: Device ID: {}, Serial: {}", device.getId(), autoGeneratedSerial);
            
            // Step 5: Create Subscription
            logger.info("🔄 Step 5: Creating Subscription...");
            subscription = new Subscription(
                    resident.getId(),
                    device.getId(),
                    providerId,
                    resource.waterTankSize().floatValue()
            );
            subscription = subscriptionContextFacade.saveSubscription(subscription);
            logger.info("✅ Step 5 completed: Subscription ID: {}", subscription.getId());
            
            // Step 6: Link Auth0 user with resident ID in Auth0 app_metadata
            logger.info("🔄 Step 6: Linking Resident ID to Auth0 user metadata...");
            auth0ManagementService.linkResidentToUser(auth0UserId, resident.getId());
            logger.info("✅ Step 6 completed: Resident linked to Auth0 user");
            
            // Step 7: Send password setup email to resident
            logger.info("🔄 Step 7: Sending password setup email...");
            String passwordResetUrl = auth0ManagementService.sendPasswordSetupEmail(auth0UserId);
            logger.info("✅ Step 7 completed: Password setup email sent");
            logger.info("🔗 Password reset URL: {}", passwordResetUrl);
            
            logger.info("🎉 All steps completed successfully!");
            
            // Return success response
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new CompleteResidentResource(
                            resident.getId(),
                            auth0UserId,
                            profile.getId(),
                            subscription.getId(),
                            device.getId(),
                            resource.email(),
                            resource.firstName(),
                            resource.lastName(),
                            resource.waterTankSize(),
                            passwordResetUrl  // Include the password reset URL
                    )
            );
            
        } catch (Auth0Exception e) {
            // Auth0 user creation failed
            logger.error("❌ Auth0 operation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    new CompleteResidentResource(
                            null, null, null, null, null,
                            resource.email(), 
                            resource.firstName() + " " + resource.lastName(),
                            resource.waterTankSize(),
                            "Failed to create Auth0 user: " + e.getMessage(),
                            null)  // No password reset URL on failure
            );
            
        } catch (Exception e) {
            // Local entity creation failed - cleanup Auth0 user if it was created
            logger.error("❌ Resident creation failed: {}", e.getMessage(), e);
            
            if (auth0UserId != null) {
                try {
                    auth0ManagementService.deleteUser(auth0UserId);
                    logger.info("🧹 Cleaned up Auth0 user: {}", auth0UserId);
                } catch (Auth0Exception cleanupEx) {
                    // Log cleanup failure but don't throw
                    logger.error("⚠️ Failed to cleanup Auth0 user: {}", cleanupEx.getMessage());
                }
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CompleteResidentResource(
                            null, null, null, null, null,
                            resource.email(), 
                            resource.firstName() + " " + resource.lastName(),
                            resource.waterTankSize(),
                            "Failed to create resident: " + e.getMessage(),
                            null)  // No password reset URL on failure
            );
        }
    }

    /**
     * Endpoint to create a new resident.
     * Only accessible by PROVIDER or ADMIN roles.
     * @param resource The request body containing resident data
     * @return ResponseEntity with the created resident resource
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResidentResource> createResident(@RequestBody CreateResidentResource resource) throws AccessDeniedException {
        // Get authenticated user id from JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOptional.get().getId();

        // Convert the resource to a command, passing the authenticated user id
        CreateResidentCommand command = CreateResidentCommandFromResourceAssembler.toCommandFromResource(resource, userId);

        // Execute the use case
        ResidentWithCredentials result = residentCommandService.handle(command);

        // Find the resident's profile using the new userId
        Long newUserId = result.resident().getUserId();
        Optional<Profile> profiles = profileQueryService.handle(new GetProfileByUserIdQuery(newUserId));
        if (profiles.isEmpty()) {
            throw new IllegalStateException("Profile for the new resident could not be found.");
        }

        // Convert the result to a resource, including generated username and password
        ResidentResource residentResource = ResidentResourceFromEntityAssembler.toResourceFromEntityWithCredentials(
                result.resident(),
                result.username(),
                result.password(),
                profiles.get()
        );

        return new ResponseEntity<>(residentResource, HttpStatus.CREATED);
    }

    /**
     * Endpoint to get all devices for a resident by resident ID.
     * Only accessible by PROVIDER or RESIDENT roles.
     * @param residentId The ID of the resident
     * @return List of device resources
     */
    @GetMapping("/{residentId}/devices")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public List<DeviceResource> getAllDevicesByResidentId(@PathVariable Long residentId) {
        return deviceContextFacade.getAllDevicesByResidentId(residentId)
                .stream()
                .map(DeviceResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Endpoint to get all subscriptions for a resident by resident ID.
     * Accessible by PROVIDER, ADMIN, or RESIDENT roles.
     * @param residentId The ID of the resident
     * @return ResponseEntity with a list of subscription resources
     */
    @GetMapping("/{residentId}/subscriptions")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<List<SubscriptionResource>> getSubscriptionsByResidentId(@PathVariable Long residentId) throws AccessDeniedException {
        var subscriptions = subscriptionContextFacade.fetchSubscriptionsByResidentId(residentId);
        if (subscriptions.isEmpty()) return ResponseEntity.notFound().build();
        var subscriptionResources = subscriptions.stream()
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new ResponseEntity<>(subscriptionResources, HttpStatus.OK);
    }

    /**
     * Endpoint to get all issue reports for a resident by resident ID.
     * Only accessible by PROVIDER or RESIDENT roles.
     * @param residentId The ID of the resident
     * @return ResponseEntity with a list of issue reports
     */
    @GetMapping("/{residentId}/issue-reports")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_RESIDENT')")
    public ResponseEntity<List<IssueReport>> getRequestsByResidentId(@PathVariable Long residentId) {
        var requests = issueReportContextFacade.fetchIssueReportsByResidentId(residentId)
                .stream()
                .filter(request -> request.getResidentId().equals(residentId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }

    /**
     * Endpoint to get all water supply requests for a resident by resident ID.
     * Only accessible by PROVIDER role.
     * @param residentId The ID of the resident
     * @return List of water supply request resources
     */
    @GetMapping("/{residentId}/water-supply-requests")
    @PreAuthorize("hasRole('ROLE_PROVIDER')")
    public List<WaterSupplyRequestResource> getWaterRequestsByResident(@PathVariable Long residentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        long userId = userOptional.get().getId();

        final Long providerId;

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"))) {
            Optional<Provider> providerOptional = providerQueryService.findByUserId(userId);
            if (providerOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found");
            }
            providerId = providerOptional.get().getId();
            List<Resident> residents = residentQueryService.handle(new GetResidentsByProviderIdQuery(providerId));
            boolean isResidentValid = residents.stream().anyMatch(resident -> resident.getId().equals(residentId));
            if (!isResidentValid) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resident does not belong to this provider");
            }
        } else {
            providerId = null;
        }

        List<WaterSupplyRequestResource> result = waterSupplyRequestContextFacade.fetchWaterRequestsByResidentId(residentId)
                .stream()
                .filter(resource -> providerId == null || resource.getProviderId().equals(providerId))
                .map(WaterRequestResourceFromAggregateAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Endpoint to get all residents for the authenticated provider or admin.
     * Only accessible by PROVIDER or ADMIN roles.
     * @return ResponseEntity with a list of resident resources
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ResidentResource>> getResidentsForAuthenticatedProviderOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long userId = userOptional.get().getId();

        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Resident> residents;

        if (isAdmin) {
            // If admin, get all residents
            residents = residentQueryService.handle(new GetAllResidentsQuery());
        } else {
            // Find the provider by userId
            Optional<Provider> providerOptional = providerQueryService.findByUserId(userId);
            if (providerOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Long providerId = providerOptional.get().getId();
            // Query residents of the provider
            residents = residentQueryService.handle(new GetResidentsByProviderIdQuery(providerId));
        }

        if (residents.isEmpty()) return ResponseEntity.notFound().build();

        List<ResidentResource> residentResources = residents.stream()
            .map(resident -> {
                // If userId is null, resident hasn't logged in yet - find profile by matching email
                if (resident.getUserId() == null) {
                    // For residents created but not yet logged in, we need to find their profile differently
                    // Since we don't have direct access to email from Resident, we'll query all profiles
                    // and match by firstName + lastName (this is a temporary solution)
                    List<Profile> allProfiles = profileQueryService.handle(new GetAllProfilesQuery());
                    Optional<Profile> matchingProfile = allProfiles.stream()
                        .filter(p -> p.getFirstName().equals(resident.getFirstName()) 
                                  && p.getLastName().equals(resident.getLastName())
                                  && p.getUserId() == null)
                        .findFirst();
                    
                    return matchingProfile
                        .map(profile -> ResidentResourceFromEntityAssembler.toResourceFromEntityWithCredentials(
                            resident, 
                            "Pending first login", 
                            null, 
                            profile))
                        .orElse(null);
                } else {
                    // Normal flow for logged-in residents
                    Optional<Profile> profileOptional = profileQueryService.handle(new GetProfileByUserIdQuery(resident.getUserId()));
                    String username = iamContextFacade.fetchUsernameByUserId(resident.getUserId());
                    return profileOptional
                        .map(profile -> ResidentResourceFromEntityAssembler.toResourceFromEntityWithCredentials(resident, username, null, profile))
                        .orElse(null);
                }
            })
            .filter(resource -> resource != null)
            .collect(Collectors.toList());

        return ResponseEntity.ok(residentResources);
    }

    /**
     * Endpoint to get the resident profile for the currently authenticated user.
     * Extracts user information from JWT token automatically.
     * Accessible by RESIDENT role.
     * @return ResponseEntity with the resident resource or NOT_FOUND if not found
     */
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('ROLE_RESIDENT')")
    public ResponseEntity<ResidentResource> getMyResidentProfile() {
        // Obtener el usuario autenticado desde el JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Extraer el auth0Id del JWT (el "sub" claim)
        String auth0Id = authentication.getName();
        
        logger.info("🔍 [/me/profile] Auth0 ID from JWT: {}", auth0Id);
        
        // Buscar el usuario en la BD local por auth0Id
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        
        if (userOptional.isEmpty()) {
            logger.error("❌ [/me/profile] No user found in DB with auth0Id: {}", auth0Id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        long userId = userOptional.get().getId();
        logger.info("✅ [/me/profile] Found User ID: {} for auth0Id: {}", userId, auth0Id);

        // Obtener el resident a partir del userId
        List<Resident> residentList = residentQueryService.findByUserId(userId);
        logger.info("🔍 [/me/profile] Residents found for userId {}: {}", userId, residentList.size());
        
        if (residentList.isEmpty()) {
            logger.warn("⚠️ [/me/profile] No resident found for userId: {}", userId);
            return ResponseEntity.notFound().build();
        }

        Resident resident = residentList.get(0);
        logger.info("✅ [/me/profile] Returning Resident ID: {} ({} {})", 
            resident.getId(), resident.getFirstName(), resident.getLastName());
        
        // Obtener el perfil asociado
        Optional<Profile> profileOptional = profileQueryService.handle(new GetProfileByUserIdQuery(userId));
        if (profileOptional.isEmpty()) {
            return ResponseEntity.internalServerError().build();
        }

        // Obtener el username del contexto IAM
        String username = iamContextFacade.fetchUsernameByUserId(userId);
        
        ResidentResource resource = ResidentResourceFromEntityAssembler
                .toResourceFromEntityWithCredentials(resident, username, null, profileOptional.get());

        return ResponseEntity.ok(resource);
    }

    /**
     * Endpoint to get resident profile by resident ID.
     * Accessible by PROVIDER, ADMIN, and RESIDENT roles.
     * @param residentId The ID of the resident
     * @return ResponseEntity with the resident resource or NOT_FOUND if not found
     */
    @GetMapping("/{residentId}/profiles")
    @PreAuthorize("hasRole('ROLE_RESIDENT') or hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResidentResource> getResidentProfileById(@PathVariable Long residentId) {
        // Buscar el resident por su ID
        Optional<Resident> residentOptional = residentQueryService.findById(residentId);
        
        if (residentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resident resident = residentOptional.get();
        long userId = resident.getUserId();

        // Obtener el perfil asociado
        Optional<Profile> profileOptional = profileQueryService.handle(new GetProfileByUserIdQuery(userId));
        if (profileOptional.isEmpty()) {
            return ResponseEntity.internalServerError().build();
        }

        // Obtener el username del contexto IAM
        String username = iamContextFacade.fetchUsernameByUserId(userId);
        
        ResidentResource resource = ResidentResourceFromEntityAssembler
                .toResourceFromEntityWithCredentials(resident, username, null, profileOptional.get());

        return ResponseEntity.ok(resource);
    }

    /**
     * Endpoint to get a resident by their ID.
     * Only accessible by PROVIDER or ADMIN roles.
     * @param residentId The ID of the resident
     * @return ResponseEntity with a list containing the resident resource or NOT_FOUND if not found
     */
    @GetMapping("/{residentId}")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<ResidentResource>> getResidentById(@PathVariable Long residentId) {
        Optional<Resident> residentOptional = residentQueryService.findById(residentId);
        if (residentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resident resident = residentOptional.get();
        
        // If resident has no userId yet (hasn't logged in), look up profile won't work
        // Return resident data with null profile
        if (resident.getUserId() == null) {
            ResidentResource residentResource = ResidentResourceFromEntityAssembler.toResourceFromEntity(resident, null);
            return ResponseEntity.ok(List.of(residentResource));
        }
        
        Optional<Profile> profiles = profileQueryService.handle(new GetProfileByUserIdQuery(resident.getUserId()));
        if (profiles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ResidentResource residentResource = ResidentResourceFromEntityAssembler.toResourceFromEntity(resident, profiles.get());
        return ResponseEntity.ok(List.of(residentResource));
    }

    /**
     * Endpoint to update a resident's profile.
     * Only accessible by RESIDENT role.
     * @param resource The request body containing updated resident data
     * @return ResponseEntity with the updated resident resource or NOT_FOUND if not found
     */
    @PutMapping("/{residentId}/profiles")
    @PreAuthorize("hasRole('ROLE_RESIDENT')")
    public ResponseEntity<ResidentResource> updateResident(@RequestBody UpdateResidentResource resource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String auth0Id = authentication.getName();
        
        var userOptional = userRepository.findByAuth0Id(auth0Id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long userId = userOptional.get().getId();

        UpdateResidentCommand updateResidentCommand = UpdateResidentCommandFromResource.toCommandFromResource(resource, userId);
        Optional<Resident> updatedResidentOptional = residentCommandService.handle(updateResidentCommand);
        if (updatedResidentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resident updatedResident = updatedResidentOptional.get();
        List<Profile> profiles = profileQueryService.handle(new GetProfileByUserIdQuery(updatedResident.getUserId()))
                .stream()
                .collect(Collectors.toList());
        ResidentResource residentResource = ResidentResourceFromEntityAssembler.toResourceFromEntity(updatedResident, profiles.get(0));
        return ResponseEntity.ok(residentResource);
    }
}
