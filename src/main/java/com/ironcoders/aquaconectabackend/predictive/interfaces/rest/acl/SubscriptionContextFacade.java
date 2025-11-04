package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl;

import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.queries.GetAllSubscriptionsByResidentId;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Service("predictiveSubscriptionContextFacade")
public class SubscriptionContextFacade {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionContextFacade.class);

    private final SubscriptionQueryService subscriptionQueryService;

    /**
     * Constructor for dependency injection.
     */
    public SubscriptionContextFacade(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    /**
     * Gets all subscriptions for a resident.
     *
     * @param residentId The resident ID
     * @return List of Subscription entities
     */
    public List<Subscription> getSubscriptionsByResidentId(Long residentId) {
        log.debug("Fetching subscriptions for resident: {}", residentId);

        try {
            return subscriptionQueryService.handle(
                    new GetAllSubscriptionsByResidentId(residentId)
            );
        } catch (AccessDeniedException e) {
            log.error("Access denied when fetching subscriptions for resident: {}",
                    residentId, e);
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching subscriptions for resident: {}", residentId, e);
            return List.of();
        }
    }

    /**
     * Gets the active subscription for a resident.
     * Returns the first active subscription found.
     *
     * @param residentId The resident ID
     * @return Optional containing the active Subscription
     */
    public Optional<Subscription> getActiveSubscriptionByResidentId(Long residentId) {
        log.debug("Fetching active subscription for resident: {}", residentId);

        List<Subscription> subscriptions = getSubscriptionsByResidentId(residentId);

        return subscriptions.stream()
                .filter(sub -> "ACTIVE".equalsIgnoreCase(sub.getStatus()))
                .findFirst();
    }

    /**
     * Gets the water tank size for a resident.
     * Retrieves it from the active subscription.
     *
     * @param residentId The resident ID
     * @return Optional containing the water tank size in liters
     */
    public Optional<Double> getWaterTankSizeByResidentId(Long residentId) {
        log.debug("Fetching water tank size for resident: {}", residentId);

        Optional<Subscription> subscription = getActiveSubscriptionByResidentId(residentId);

        if (subscription.isPresent()) {
            Float tankSize = subscription.get().getWaterTankSize();
            Double size = tankSize != null ? tankSize.doubleValue() : null;

            log.debug("Water tank size for resident {}: {} liters", residentId, size);
            return Optional.ofNullable(size);
        }

        log.warn("No active subscription found for resident: {}", residentId);
        return Optional.empty();
    }

    /**
     * Gets the sensor/device ID from the subscription.
     *
     * @param residentId The resident ID
     * @return Optional containing the sensor ID
     */
    public Optional<Long> getSensorIdByResidentId(Long residentId) {
        log.debug("Fetching sensor ID for resident: {}", residentId);

        Optional<Subscription> subscription = getActiveSubscriptionByResidentId(residentId);

        return subscription.map(Subscription::getSensorId);
    }

    /**
     * Checks if a resident has an active subscription.
     *
     * @param residentId The resident ID
     * @return true if resident has active subscription, false otherwise
     */
    public boolean hasActiveSubscription(Long residentId) {
        return getActiveSubscriptionByResidentId(residentId).isPresent();
    }
}