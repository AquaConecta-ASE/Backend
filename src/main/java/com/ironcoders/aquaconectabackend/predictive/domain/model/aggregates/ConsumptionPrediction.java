package com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates;


import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name= "consumption_prediction")
public class ConsumptionPrediction extends AuditableAbstractAggregateRoot<ConsumptionPrediction> {

    /**
     * ID of the subscription (primary identifier for predictions)
     * Each subscription represents one water tank/sensor
     */
    @Column(nullable = false, name = "subscription_id")
    private Long subscriptionId;

    /**
     * ID of the resident for whom the prediction was made
     * Kept for secondary queries (get all predictions for a resident)
     */
    @Column(nullable = false, name = "resident_id")
    private Long residentId;

    /**
     * ID of the device/sensor associated with this prediction
     */
    @Column(name = "device_id")
    private Long deviceId;

    /**
     * Date and time when the prediction was generated
     */
    @Column(nullable = false, name = "prediction_date")
    private LocalDateTime predictionDate;

    /**
     * Average daily consumption (in liters)
     */
    @Column(nullable = false, name = "daily_average_consumption")
    private Double dailyAverageConsumption;

    /**
     * JSON string containing predictions for the next 7 days
     * Format: [{"date":"2025-10-28","predictedConsumption":45.5,"dayOfWeek":"Monday"},...]
     */
    @Column(nullable = false, columnDefinition = "TEXT", name = "predictions_json")
    private String predictionsJson;

    /**
     * Estimated date when water will run out
     */
    @Column(name = "water_runout_date")
    private LocalDate waterRunoutDate;

    /**
     * Number of days until water runs out
     */
    @Column(name = "days_until_runout")
    private Integer daysUntilRunout;

    /**
     * Confidence score of the prediction (0.0 to 1.0)
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * Current water level at the time of prediction
     */
    @Column(name = "current_water_level")
    private Double currentWaterLevel;

    /**
     * Total predicted consumption for the next 7 days
     */
    @Column(name = "total_predicted_consumption_7days")
    private Double totalPredictedConsumption7Days;

    /**
     * Status of the prediction (ACTIVE, OUTDATED, ARCHIVED)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PredictionStatus status;

    /**
     * Default constructor for JPA
     */
    public ConsumptionPrediction() {
        this.status = PredictionStatus.ACTIVE;
        this.predictionDate = LocalDateTime.now();
    }

    /**
     * Constructor for creating a new prediction
     */
    public ConsumptionPrediction(Long subscriptionId, Long residentId, Long deviceId,
                                 Double dailyAverageConsumption,
                                 String predictionsJson, LocalDate waterRunoutDate,
                                 Integer daysUntilRunout, Double confidenceScore,
                                 Double currentWaterLevel, Double totalPredicted) {
        this();
        this.subscriptionId = subscriptionId;
        this.residentId = residentId;
        this.deviceId = deviceId;
        this.dailyAverageConsumption = dailyAverageConsumption;
        this.predictionsJson = predictionsJson;
        this.waterRunoutDate = waterRunoutDate;
        this.daysUntilRunout = daysUntilRunout;
        this.confidenceScore = confidenceScore;
        this.currentWaterLevel = currentWaterLevel;
        this.totalPredictedConsumption7Days = totalPredicted;
    }

    /**
     * Marks this prediction as outdated
     */
    public void markAsOutdated() {
        this.status = PredictionStatus.OUTDATED;
    }

    /**
     * Checks if this prediction is still valid (less than 24 hours old)
     */
    public boolean isValid() {
        return this.status == PredictionStatus.ACTIVE &&
                this.predictionDate.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Status of a prediction
     */
    public enum PredictionStatus {
        ACTIVE,      // Currently valid
        OUTDATED,    // Too old, needs refresh
        ARCHIVED     // Historical record
    }

}
