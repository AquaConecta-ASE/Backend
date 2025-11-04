package com.ironcoders.aquaconectabackend.monitoring.domain.services;


import java.util.List;
import java.util.Optional;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllEventsBySensorId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetEventByIdQuery;

public interface EventQueryService {
    Optional<Event> handle(GetEventByIdQuery query);
    List<Event> handle(GetAllEventsBySensorId query);
}