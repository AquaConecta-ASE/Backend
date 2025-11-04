package com.ironcoders.aquaconectabackend.predictive.application.internal;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetConsumptionHistoryQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetLatestPredictionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionByResidentIdQuery;
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
     * Gets the latest VALID prediction for a resident.
     * Valid = ACTIVE status AND less than 24 hours old.
     *
     * @param query Query containing resident ID
     * @return Optional containing the latest valid prediction
     */
    @Override
    public Optional<ConsumptionPrediction> handle(GetLatestPredictionQuery query) {
        log.debug("Getting latest valid prediction for resident: {}", query.residentId());

        // Try to find active prediction
        Optional<ConsumptionPrediction> prediction =
                predictionRepository.findFirstByResidentIdAndStatusOrderByPredictionDateDesc(
                        query.residentId(),
                        ConsumptionPrediction.PredictionStatus.ACTIVE
                );

        // Check if prediction is still valid (less than 24h old)
        if (prediction.isPresent()) {
            ConsumptionPrediction pred = prediction.get();

            if (pred.isValid()) {
                log.debug("Found valid prediction for resident: {} (generated at: {})",
                        query.residentId(), pred.getPredictionDate());
                return prediction;
            } else {
                log.debug("Prediction found but is outdated (older than 24h) for resident: {}",
                        query.residentId());
                return Optional.empty();
            }
        }

        log.debug("No valid prediction found for resident: {}", query.residentId());
        return Optional.empty();
    }

    /**
     * Gets consumption history for a resident in a date range.
     *
     * @param query Query containing resident ID, start date, and end date
     * @return List of WaterConsumption records ordered by date
     */
    @Override
    public List<WaterConsumption> handle(GetConsumptionHistoryQuery query) {
        log.debug("Getting consumption history for resident: {} from {} to {}",
                query.residentId(), query.startDate(), query.endDate());

        return consumptionRepository.findByResidentIdAndDateBetweenOrderByDateAsc(
                query.residentId(),
                query.startDate(),
                query.endDate()
        );
    }
}
