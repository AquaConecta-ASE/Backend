package com.ironcoders.aquaconectabackend.predictive.domain.services;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetConsumptionHistoryQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetLatestPredictionQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionByResidentIdQuery;
import com.ironcoders.aquaconectabackend.predictive.domain.model.queries.GetPredictionHistoryBySubscriptionQuery;

import java.util.List;
import java.util.Optional;

public interface PredictionQueryService {

    Optional<ConsumptionPrediction> handle(GetPredictionByResidentIdQuery query);

    Optional<ConsumptionPrediction> handle(GetLatestPredictionQuery query);

    List<WaterConsumption> handle(GetConsumptionHistoryQuery query);

    List<ConsumptionPrediction> handle(GetPredictionHistoryBySubscriptionQuery query);

}
