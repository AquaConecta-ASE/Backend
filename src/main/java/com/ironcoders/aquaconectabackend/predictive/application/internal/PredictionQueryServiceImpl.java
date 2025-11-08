package com.ironcoders.aquaconectabackend.predictive.application.internal;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetConsumptionHistoryQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetLatestPredictionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionByResidentIdQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionHistoryBySubscriptionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.services.PredictionQueryService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.ConsumptionPredictionRepository;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.WaterConsumptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PredictionQueryServiceImpl implements PredictionQueryService {

    private static final Logger log = LoggerFactory.getLogger(PredictionQueryServiceImpl.class);

    private final ConsumptionPredictionRepository predictionRepository;
    private final WaterConsumptionRepository consumptionRepository;

    /**
     * Constructor for dependency injection.
     */
    public PredictionQueryServiceImpl(
            ConsumptionPredictionRepository predictionRepository,
            WaterConsumptionRepository consumptionRepository) {
        this.predictionRepository = predictionRepository;
        this.consumptionRepository = consumptionRepository;
    }

    /**
     * Gets the latest prediction for a resident (regardless of status or age).
     *
     * @param query Query containing resident ID
     * @return Optional containing the most recent prediction
     */
    @Override
    public Optional<ConsumptionPrediction> handle(GetPredictionByResidentIdQuery query) {
        log.debug("Getting latest prediction for resident: {}", query.residentId());

        return predictionRepository.findFirstByResidentIdOrderByPredictionDateDesc(
                query.residentId()
        );
    }

    /**
     * PRIMARY: Gets the latest VALID prediction for a subscription.
     * Valid = ACTIVE status AND less than 24 hours old.
     *
     * @param query Query containing subscriptionId
     * @return Optional containing the latest valid prediction
     */
    @Override
    public Optional<ConsumptionPrediction> handle(GetLatestPredictionQuery query) {
        log.debug("Getting latest valid prediction for subscription: {}", query.subscriptionId());

        // Try to find active prediction for this subscription
        Optional<ConsumptionPrediction> prediction =
                predictionRepository.findFirstBySubscriptionIdAndStatusOrderByPredictionDateDesc(
                        query.subscriptionId(),
                        ConsumptionPrediction.PredictionStatus.ACTIVE
                );

        // Check if prediction is still valid (less than 24h old)
        if (prediction.isPresent()) {
            ConsumptionPrediction pred = prediction.get();

            if (pred.isValid()) {
                log.debug("Found valid prediction for subscription: {} (generated at: {})",
                        query.subscriptionId(), pred.getPredictionDate());
                return prediction;
            } else {
                log.debug("Prediction found but is outdated (older than 24h) for subscription: {}",
                        query.subscriptionId());
                return Optional.empty();
            }
        }

        log.debug("No valid prediction found for subscription: {}", query.subscriptionId());
        return Optional.empty();
    }

    /**
     * PRIMARY: Gets consumption history for a subscription in a date range.
     *
     * @param query Query containing subscriptionId, start date, and end date
     * @return List of WaterConsumption records ordered by date
     */
    @Override
    public List<WaterConsumption> handle(GetConsumptionHistoryQuery query) {
        log.debug("Getting consumption history for subscription: {} from {} to {}",
                query.subscriptionId(), query.startDate(), query.endDate());

        return consumptionRepository.findBySubscriptionIdAndDateBetweenOrderByDateAsc(
                query.subscriptionId(),
                query.startDate(),
                query.endDate()
        );
    }

    /**
     * Gets all prediction history for a subscription (regardless of status).
     * Returns predictions ordered by date descending (newest first).
     * Useful for providers to see complete prediction history.
     *
     * @param query Query containing subscriptionId
     * @return List of all ConsumptionPrediction records for the subscription
     */
    @Override
    public List<ConsumptionPrediction> handle(GetPredictionHistoryBySubscriptionQuery query) {
        log.debug("Getting prediction history for subscription: {}", query.subscriptionId());

        return predictionRepository.findBySubscriptionIdOrderByPredictionDateDesc(
                query.subscriptionId()
        );
    }
}
