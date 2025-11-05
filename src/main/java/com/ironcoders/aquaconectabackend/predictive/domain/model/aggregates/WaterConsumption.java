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

    public WaterConsumption(Long residentId, LocalDate date, Double consumption,
                            Long deviceId, Double initialLevel, Double finalLevel,
                            String waterQuality, Boolean isRefill) {
        this.residentId = residentId;
        this.date = date;
        this.consumption = consumption;
        this.deviceId = deviceId;
        this.initialLevel = initialLevel;
        this.finalLevel = finalLevel;
        this.waterQuality = waterQuality;
        this.isRefill = isRefill;
    }

    public WaterConsumption(Long residentId, LocalDate date, Double consumption, 
                           Long deviceId, Double initialLevel, Double finalLevel, 
                           String waterQuality) {
        this(residentId, date, consumption, deviceId, initialLevel, finalLevel, waterQuality, false);
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
}
