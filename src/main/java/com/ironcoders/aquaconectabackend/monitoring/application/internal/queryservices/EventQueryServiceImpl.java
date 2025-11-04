package com.ironcoders.aquaconectabackend.monitoring.application.internal.queryservices;
import org.springframework.stereotype.Service;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllEventsBySensorId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetEventByIdQuery;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventQueryService;
import com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories.EventRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;

    public EventQueryServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Optional<Event> handle(GetEventByIdQuery query) {
        return eventRepository.findById(query.eventId()); }

    @Override
    public List<Event> handle(GetAllEventsBySensorId query) {
        return eventRepository.findBySensorId(query.sensorId());
    }


}