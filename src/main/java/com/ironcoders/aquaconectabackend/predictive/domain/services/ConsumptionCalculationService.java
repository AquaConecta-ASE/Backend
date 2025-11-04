package com.ironcoders.aquaconectabackend.predictive.domain.services;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;

import java.util.List;
import java.util.Optional;

public interface ConsumptionCalculationService {

    List<WaterConsumption> handle(CalculateDailyConsumptionCommand command);

}
