package com.ironcoders.aquaconectabackend.predictive.domain.services;

import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;

import java.util.List;

public interface ConsumptionCalculationService {

    List<WaterConsumption> handle(CalculateDailyConsumptionCommand command);

}
