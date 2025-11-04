package com.ironcoders.aquaconectabackend.requests.domain.services;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateWaterSupplyRequestCommand;

public interface WaterSupplyRequestCommandService {
    Optional<WaterSupplyRequest> handle(CreateWaterSupplyRequestCommand command) throws AccessDeniedException;
    Optional<WaterSupplyRequest> handle(UpdateWaterSupplyRequestCommand command) throws AccessDeniedException;
}