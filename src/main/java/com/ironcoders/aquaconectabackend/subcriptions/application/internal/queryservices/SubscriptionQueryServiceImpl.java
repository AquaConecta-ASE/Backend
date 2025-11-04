package com.ironcoders.aquaconectabackend.subcriptions.application.internal.queryservices;

import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptions;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptionsByResidentId;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetSubscriptionByUserId;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetSubscriptionsByProviderId;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionQueryService;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;


    public SubscriptionQueryServiceImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;

    }

    @Override
    public List<Subscription> handle(GetAllSubscriptionsByResidentId query) throws AccessDeniedException {

        List<Subscription> subscriptions = subscriptionRepository.findByResidentId(query.residentId());

        System.out.println("🔎 Subscripciones encontradas: " + subscriptions.size());
        subscriptions.forEach(s -> System.out.println("📦 Subscription ID: " + s.getId()));
        if (subscriptions.isEmpty()) {
            throw new AccessDeniedException("Este residente no tiene suscripciones.");
        }

        return subscriptions;
    }


    @Override
    public Optional<Subscription> handle(GetSubscriptionByUserId query) {
        return Optional.empty(); // puedes implementar la lógica aquí después
    }
    @Override
    public List<Subscription> handle(GetAllSubscriptions query) throws AccessDeniedException {
      
        return subscriptionRepository.findAll();

    }

    @Override
    public List<Subscription> handle(GetSubscriptionsByProviderId query) throws AccessDeniedException {
        return subscriptionRepository.findByProviderId(query.providerId());
    }
    


}
