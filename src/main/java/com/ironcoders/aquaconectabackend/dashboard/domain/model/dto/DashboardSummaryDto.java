package com.ironcoders.aquaconectabackend.dashboard.domain.model.dto;

/**
 * Data Transfer Object for dashboard summary statistics.
 */
public class DashboardSummaryDto {
    private long totalProviders;
    private long totalResidents;
    private long activeSubscriptions;
    private float totalIncome;
    private float monthlyIncome;

    /**
     * Full constructor for DashboardSummaryDto.
     * @param totalProviders Total number of providers
     * @param totalResidents Total number of residents
     * @param activeSubscriptions Number of active subscriptions
     * @param totalIncome Total income from all subscriptions
     * @param monthlyIncome Income for the current month
     */
    public DashboardSummaryDto(long totalProviders, long totalResidents,
                               long activeSubscriptions, float totalIncome, float monthlyIncome) {
        this.totalProviders = totalProviders;
        this.totalResidents = totalResidents;
        this.activeSubscriptions = activeSubscriptions;
        this.totalIncome = totalIncome;
        this.monthlyIncome = monthlyIncome;
    }

    /**
     * Empty constructor.
     */
    public DashboardSummaryDto() {}

    // Getters and setters
    public long getTotalProviders() {
        return totalProviders;
    }

    public void setTotalProviders(long totalProviders) {
        this.totalProviders = totalProviders;
    }

    public long getTotalResidents() {
        return totalResidents;
    }

    public void setTotalResidents(long totalResidents) {
        this.totalResidents = totalResidents;
    }

    public long getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public void setActiveSubscriptions(long activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
    }

    public float getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(float totalIncome) {
        this.totalIncome = totalIncome;
    }

    public float getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(float monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }
}
