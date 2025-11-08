package com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates;


import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "water_consumptions")
public class WaterConsumption extends AuditableAbstractAggregateRoot<WaterConsumption> {

    /**
     * ID of the subscription (primary identifier)
     * Each subscription represents one water tank/sensor
     */
    @Column(nullable = false, name = "subscription_id")
    private Long subscriptionId;

    /**
     * ID of the resident (kept for secondary queries)
     */
    @Column(nullable = false, name = "resident_id")
    private Long residentId;

    @Column(nullable = false, name = "consumption_date")
    private LocalDate date;

    /**
     * Amount of water consumed in liters
     */
    @Column(nullable = false)
    private Double consumption;

    /**
     * Device/Sensor that measured the consumption
     */
    @Column(nullable = false, name = "device_id")
    private Long deviceId;

    /**
     * Water quality on that day (from Events)
     */
    @Column(name = "water_quality")
    private String waterQuality;

    /**
     * Initial water level at the start of the day
     */
    @Column(name = "initial_level")
    private Double initialLevel;

    /**
     * Final water level at the end of the day
     */
    @Column(name = "final_level")
    private Double finalLevel;

    @Column(nullable = false, name = "is_refill")
    private Boolean isRefill;
    /**
     * Default constructor for JPA
     */
    public WaterConsumption() {
    }

    public WaterConsumption(Long subscriptionId, Long residentId, LocalDate date, Double consumption,
                            Long deviceId, Double initialLevel, Double finalLevel,
                            String waterQuality, Boolean isRefill) {
        this.subscriptionId = subscriptionId;
        this.residentId = residentId;
        this.date = date;
        this.consumption = consumption;
        this.deviceId = deviceId;
        this.initialLevel = initialLevel;
        this.finalLevel = finalLevel;
        this.waterQuality = waterQuality;
        this.isRefill = isRefill;
    }

    public WaterConsumption(Long subscriptionId, Long residentId, LocalDate date, Double consumption, 
                           Long deviceId, Double initialLevel, Double finalLevel, 
                           String waterQuality) {
        this(subscriptionId, residentId, date, consumption, deviceId, initialLevel, finalLevel, waterQuality, false);
    }

    /**
     * Calculate consumption based on level difference
     * consumption = initialLevel - finalLevel
     */
    public void calculateConsumption() {
        if (this.initialLevel != null && this.finalLevel != null) {
            this.consumption = Math.abs(this.initialLevel - this.finalLevel);
        }
    }

    /**
     * Updates the water quality
     */
    public void updateWaterQuality(String quality) {
        this.waterQuality = quality;
    }

    public void detectRefill() {
        if (this.initialLevel != null && this.finalLevel != null) {
            this.isRefill = this.finalLevel > this.initialLevel;
        }
    }

    /**
     * Updates consumption data when new events are added.
     * This allows recalculation of consumption and refill detection.
     * 
     * IMPORTANT: The isRefill flag is determined by the service layer
     * (ConsumptionCalculationServiceImpl) which analyzes ALL events of the day,
     * not just initial and final levels.
     * 
     * @param initialLevel Initial water level (in liters)
     * @param finalLevel Final water level (in liters)
     * @param waterQuality Water quality for the day
     * @param deviceId Device that recorded the measurements
     */
    public void updateConsumptionData(Double initialLevel, Double finalLevel, 
                                      String waterQuality, Long deviceId) {
        this.initialLevel = initialLevel;
        this.finalLevel = finalLevel;
        this.waterQuality = waterQuality;
        this.deviceId = deviceId;
        
        // Simple detection based on final vs initial
        // (The service layer does more sophisticated detection using intermediate events)
        boolean likelyRefill = finalLevel > initialLevel;
        
        if (likelyRefill) {
            // Likely a refill day (but service layer will confirm)
            this.isRefill = true;
            this.consumption = 0.0;
            log.debug("🔄 Updated as REFILL day: {}L → {}L", initialLevel, finalLevel);
        } else {
            // Normal consumption day
            this.isRefill = false;
            this.consumption = Math.abs(initialLevel - finalLevel);
            log.debug("✅ Updated consumption: {} L ({}L → {}L)", 
                this.consumption, initialLevel, finalLevel);
        }
    }
    
    /**
     * Manually set the refill flag.
     * Used when the service layer detects a refill through intermediate events.
     * 
     * @param isRefill true if this day had a refill
     */
    public void setRefillStatus(boolean isRefill) {
        this.isRefill = isRefill;
        if (isRefill) {
            this.consumption = 0.0;
            log.debug("🔄 Manually marked as REFILL day");
        }
    }
    
    private static final org.slf4j.Logger log = 
        org.slf4j.LoggerFactory.getLogger(WaterConsumption.class);
}
