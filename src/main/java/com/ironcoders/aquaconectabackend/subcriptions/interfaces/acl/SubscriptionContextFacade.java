package com.ironcoders.aquaconectabackend.subcriptions.interfaces.acl;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptionsByResidentId;
import com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription.SubscriptionCommandService;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionQueryService;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service("subscriptionContextFacade")
public class SubscriptionContextFacade {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionContextFacade(SubscriptionCommandService subscriptionCommandService, SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;       
    }

    /**
     * Creates a new subscription for a resident.
     *
     * @param command the information to create the subscription
     * @return the created Subscription
     */
    public Optional<Subscription> createSubscription(CreateSubscriptionCommand command) {
        return subscriptionCommandService.handle(command);
    }

    /**
     * Fetches a subscription by its ID.
     *
     * @param subscriptionId the ID of the subscription
     * @return an Optional containing the Subscription if found, otherwise empty
     * @throws AccessDeniedException 
     */
     public List<Subscription> fetchSubscriptionsByResidentId(Long residentId) throws AccessDeniedException {
         return subscriptionQueryService.handle(new GetAllSubscriptionsByResidentId(residentId));
     }
  }