package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllEventsBySensorId;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventCommandService;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventQueryService;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.CreateEventResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.EventResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform.CreateEventCommandFromResourceAssembler;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform.EventResourceFromEntityAssembler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for event-related endpoints.
 * Provides endpoints to create and manage events.
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name="Events", description = "Event Management endpoints")
@PreAuthorize("isAuthenticated()")
public class EventController {

    private final EventCommandService eventCommandService;
    private final EventQueryService eventQueryService;

    /**
     * Constructor for dependency injection.
     * @param eventCommandService Service for event commands
     * @param eventQueryService Service for event queries
     */
    public EventController(EventCommandService eventCommandService, EventQueryService eventQueryService) {
        this.eventCommandService = eventCommandService;
        this.eventQueryService = eventQueryService;
    }

    /**
     * Endpoint to create a new event.
     * @param resource The request body containing event data
     * @return ResponseEntity with the created event resource or BAD_REQUEST if creation fails
     */
    @PostMapping
    public ResponseEntity<EventResource> createEvent(@RequestBody CreateEventResource resource){
        // Convert the resource to a command
        CreateEventCommand createEventCommand = CreateEventCommandFromResourceAssembler.toCommandFromResource(resource);
        // Handle the command to create the event
        var event = eventCommandService.handle(createEventCommand);
        if (event.isEmpty())   return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        // Convert the created event to a resource
        var eventResource= EventResourceFromEntityAssembler.toResourceFromEntity(event.get());
        return new ResponseEntity<>(eventResource, HttpStatus.CREATED);
    }
}


