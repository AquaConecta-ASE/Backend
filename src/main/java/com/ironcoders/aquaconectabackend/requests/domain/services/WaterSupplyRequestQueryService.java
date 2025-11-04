package com.ironcoders.aquaconectabackend.requests.domain.services;

import com.ironcoders.aquaconectabackend.profiles.domain.model.queries.GetWaterRequestsByResidentIdQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetAllWaterSupplyRequestsQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetWaterSupplyRequestByIdQuery;
import com.ironcoders.aquaconectabackend.requests.domain.model.queries.GetWaterSupplyRequestsByResidentIdQuery;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface WaterSupplyRequestQueryService {
    Optional<WaterSupplyRequest> handle(GetWaterSupplyRequestByIdQuery query);
    List<WaterSupplyRequest> handle(GetAllWaterSupplyRequestsQuery query);
    List<WaterSupplyRequest> handle(GetWaterRequestsByResidentIdQuery getWaterRequestsByResidentIdQuery);
}