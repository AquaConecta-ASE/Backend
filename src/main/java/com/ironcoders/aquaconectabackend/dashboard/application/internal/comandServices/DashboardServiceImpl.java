package com.ironcoders.aquaconectabackend.dashboard.application.internal.comandServices;

import com.ironcoders.aquaconectabackend.dashboard.domain.model.dto.DashboardSummaryDto;
import com.ironcoders.aquaconectabackend.dashboard.domain.services.DashboardQueryService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;

/**
 * Service implementation for dashboard queries.
 * Provides summary statistics for the dashboard view.
 */
@Service
public class DashboardServiceImpl implements DashboardQueryService {
    private final ProviderRepository providerRepository;
    private final ResidentRepository residentRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Constructor for dependency injection.
     * @param providerRepository Repository for providers
     * @param residentRepository Repository for residents
     * @param subscriptionRepository Repository for subscriptions
     */
    public DashboardServiceImpl(ProviderRepository providerRepository, ResidentRepository residentRepository, SubscriptionRepository subscriptionRepository) {
        this.providerRepository = providerRepository;
        this.residentRepository = residentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Retrieves dashboard summary statistics.
     * @return DashboardSummaryDto containing totals and income
     */
    @Override
    public DashboardSummaryDto getDashboard() {
        // Get total number of providers
        long totalProviders = providerRepository.count();
        // Get total number of residents
        long totalResidents = residentRepository.count();
        // Get total number of active subscriptions
        long activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");
        // Get total income from all subscriptions
        Float totalIncome = subscriptionRepository.sumAllPrices();
        // Get income for the current month
        Float monthlyIncome = subscriptionRepository.sumCurrentMonthPrices();

        return new DashboardSummaryDto(
                totalProviders,
                totalResidents,
                activeSubscriptions,
                totalIncome != null ? totalIncome : 0f,
                monthlyIncome != null ? monthlyIncome : 0f
        );
    }
}
