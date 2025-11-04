package com.ironcoders.aquaconectabackend.monitoring.application.internal.commandservices;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.UpdateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventCommandService;
import com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories.EventRepository;

import java.util.Optional;

/**
 * Service implementation for event command operations.
 * Handles creation and update of Event entities.
 */
@Service
public class EventCommandServiceImpl implements EventCommandService {

    private final EventRepository eventRepository;

    /**
     * Constructor for dependency injection.
     * @param eventRepository Repository for Event entities
     */
    public EventCommandServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Handles the creation of a new Event based on the provided command.
     * Retrieves the authenticated user from the security context.
     * @param command The command containing event creation data
     * @return An Optional containing the created Event
     */
    @Override
    public Optional<Event> handle(CreateEventCommand command){
        // Get the current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Create a new Event entity from the command
        var eventAggregate = new Event(command);

        // Save the event to the repository
        eventRepository.save(eventAggregate);
        // Return the created event wrapped in an Optional
        return Optional.of(eventAggregate);
    }

    /**
     * Handles the update of an existing Event based on the provided command.
     * Currently not implemented.
     * @param command The command containing event update data
     * @return An empty Optional
     */
    @Override
    public Optional<Event> handle(UpdateEventCommand command) {
        return Optional.empty();
    }
}
