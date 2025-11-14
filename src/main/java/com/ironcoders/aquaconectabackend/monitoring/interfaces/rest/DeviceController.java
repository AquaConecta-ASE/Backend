package com.ironcoders.aquaconectabackend.monitoring.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllEventsBySensorId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByIdQuery;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetDeviceByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceQueryService;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventQueryService;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.DeviceResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.resources.EventResource;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform.DeviceResourceFromEntityAssembler;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.transform.EventResourceFromEntityAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for device-related endpoints.
 * Provides endpoints to retrieve device and event information.
 */
@RestController
@RequestMapping(value = "/api/v1/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Devices", description = "Device Management endpoints")
@PreAuthorize("isAuthenticated()")
public class DeviceController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceQueryService deviceQueryService;
    private final EventQueryService eventQueryService;

    /**
     * Constructor for dependency injection.
     * @param deviceQueryService Service for device queries
     * @param eventQueryService Service for event queries
     */
    public DeviceController(DeviceQueryService deviceQueryService, EventQueryService eventQueryService) {
        this.deviceQueryService = deviceQueryService;
        this.eventQueryService = eventQueryService;
    }

    /**
     * Retrieves a device by its ID.
     * @param id The ID of the device
     * @return ResponseEntity with the device resource or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResource> getSensorByResidentId(@PathVariable Long id) {
        return deviceQueryService.handle(new GetDeviceByIdQuery(id))
                .map(DeviceResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all events for a given sensor (device) ID.
     * @param id The ID of the sensor (device)
     * @return ResponseEntity with a list of event resources
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<EventResource>> getEventsBySensorId(@PathVariable Long id) {
        LOGGER.info("📊 Obteniendo eventos para device ID: {}", id);
        
        var events = eventQueryService.handle(new GetAllEventsBySensorId(id));
        LOGGER.info("✅ Eventos encontrados: {}", events.size());
        
        var resources = events.stream()
                .map(EventResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        
        LOGGER.info("📦 Recursos creados: {}", resources.size());
        LOGGER.info("🚀 Devolviendo respuesta 200 OK con {} eventos", resources.size());
        
        return ResponseEntity.ok(resources);
    }
}
