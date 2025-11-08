package com.ironcoders.aquaconectabackend.predictive.domain.services;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.GeneratePredictionCommand;

import java.util.Optional;

public interface PredictionCommandService {
    Optional<ConsumptionPrediction> handle(GeneratePredictionCommand command);


}
