package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateAdditionalSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.UpdateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptions;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetSubscriptionsByProviderId;
import com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription.SubscriptionCommandService;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionQueryService;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources.SubscriptionResource;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources.UpdateSubscriptionResource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Subscriptions", description = "Subscription Management Endpoints")
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final ProviderContextFacade providerContextFacade;
    private final UserRepository userRepository;

    public SubscriptionController(SubscriptionCommandService subscriptionCommandService, SubscriptionQueryService subscriptionQueryService, ProviderContextFacade providerContextFacade, UserRepository userRepository) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.providerContextFacade = providerContextFacade;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<SubscriptionResource> createSubscription(@RequestBody CreateAdditionalSubscriptionCommand resource) {
        CreateAdditionalSubscriptionCommand createSubscriptionCommand= CreateSubscriptionCommandFromResourceAssembler.toCommandFromResource(resource);
      
        var subscription= subscriptionCommandService.handle(createSubscriptionCommand);
        if (subscription.isEmpty())return ResponseEntity.badRequest().build();
        var subscriptionResource = SubscriptionResourceFromEntityAssembler.toResourceFromEntity(subscription.get());
        return new ResponseEntity<>(subscriptionResource, HttpStatus.CREATED);
    }



    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<SubscriptionResource>> getSubscriptions() throws AccessDeniedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        List<Subscription> subscriptions;

        if (isAdmin) {
            // Devuelve todas las suscripciones
            subscriptions = subscriptionQueryService.handle(new GetAllSubscriptions());
        } else {
            // Solo devuelve las suscripciones del provider autenticado
            String auth0Id = auth.getName();
            
            var userOptional = userRepository.findByAuth0Id(auth0Id);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Long userId = userOptional.get().getId();

            // Obtiene el providerId del usuario autenticado
            Long providerId = providerContextFacade.fetchProviderByUserId(userId)
                    .orElseThrow(() -> new AccessDeniedException("No provider found for the authenticated user."))
                    .getId();

            subscriptions = subscriptionQueryService.handle(new GetSubscriptionsByProviderId(providerId));
        }

        if (subscriptions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        var subscriptionResources = subscriptions.stream()
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(subscriptionResources);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PROVIDER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<SubscriptionResource> updateSubscription(
            @PathVariable Long id,
            @RequestBody UpdateSubscriptionResource resource
    ) {
        UpdateSubscriptionCommand command = UpdateSubscriptionCommandFromResource.toCommand(id, resource);

        Optional<Subscription> updated = subscriptionCommandService.handle(command);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Subscription agg = updated.get();
        SubscriptionResource responseBody = SubscriptionResourceFromEntityAssembler.toResourceFromEntity(agg);
        return ResponseEntity.ok(responseBody);
    }



}

