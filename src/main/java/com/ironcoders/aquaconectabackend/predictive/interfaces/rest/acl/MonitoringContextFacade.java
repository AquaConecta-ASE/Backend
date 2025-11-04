package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllDevicesByResidentId;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.queries.GetAllEventsBySensorId;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.DeviceQueryService;
import com.ironcoders.aquaconectabackend.monitoring.domain.services.EventQueryService;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto.DeviceDTO;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto.EventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MonitoringContextFacade {

    private static final Logger log = LoggerFactory.getLogger(MonitoringContextFacade.class);

    private final DeviceQueryService deviceQueryService;
    private final EventQueryService eventQueryService;

    /**
     * Constructor for dependency injection.
     */
    public MonitoringContextFacade(
            DeviceQueryService deviceQueryService,
            EventQueryService eventQueryService) {
        this.deviceQueryService = deviceQueryService;
        this.eventQueryService = eventQueryService;
    }

    /**
     * Gets all devices for a resident.
     *
     * @param residentId The resident ID
     * @return List of DeviceDTO
     */
    public List<DeviceDTO> getDevicesByResidentId(Long residentId) {
        log.debug("Fetching devices for resident: {}", residentId);

        try {
            List<Device> devices = deviceQueryService.handle(
                    new GetAllDevicesByResidentId(residentId)
            );

            return devices.stream()
                    .map(this::toDeviceDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching devices for resident: {}", residentId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets the primary (first) device for a resident.
     *
     * @param residentId The resident ID
     * @return Optional containing DeviceDTO
     */
    public Optional<DeviceDTO> getPrimaryDeviceByResidentId(Long residentId) {
        log.debug("Fetching primary device for resident: {}", residentId);

        List<DeviceDTO> devices = getDevicesByResidentId(residentId);

        return devices.isEmpty() ? Optional.empty() : Optional.of(devices.get(0));
    }

    /**
     * Gets all events for a resident in a date range.
     * This method:
     * 1. Gets all devices for the resident
     * 2. For each device, gets all events
     * 3. Filters events by date range
     * 4. Transforms to EventDTO
     *
     * @param residentId The resident ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of EventDTO ordered by timestamp
     */
    public List<EventDTO> getEventsByResidentId(
            Long residentId,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("Fetching events for resident: {} from {} to {}",
                residentId, startDate, endDate);

        try {
            // 1. Get all devices for the resident
            List<Device> devices = deviceQueryService.handle(
                    new GetAllDevicesByResidentId(residentId)
            );

            if (devices.isEmpty()) {
                log.warn("No devices found for resident: {}", residentId);
                return new ArrayList<>();
            }

            // 2. Get events for all devices
            List<EventDTO> allEvents = new ArrayList<>();
            int totalEventsBeforeFilter = 0;

            for (Device device : devices) {
                List<Event> deviceEvents = eventQueryService.handle(
                        new GetAllEventsBySensorId(device.getId())
                );

                totalEventsBeforeFilter += deviceEvents.size();
                
                log.debug("Device {}: Found {} raw events", device.getId(), deviceEvents.size());

                // 3. Transform and filter events by date range
                List<EventDTO> filteredEvents = deviceEvents.stream()
                        .map(event -> toEventDTO(event, device.getId()))
                        .filter(eventDTO -> {
                            boolean inRange = isInDateRange(eventDTO, startDate, endDate);
                            if (!inRange) {
                                log.debug("Event {} EXCLUDED: date {} is outside range [{} - {}]",
                                    eventDTO.getId(), eventDTO.getDate(), startDate, endDate);
                            }
                            return inRange;
                        })
                        .collect(Collectors.toList());

                log.debug("Device {}: {} events after date filter", device.getId(), filteredEvents.size());
                allEvents.addAll(filteredEvents);
            }
            
            log.info("Total events before filter: {}, after filter: {} (excluded: {})",
                totalEventsBeforeFilter, allEvents.size(), totalEventsBeforeFilter - allEvents.size());

            // 4. Sort by timestamp
            allEvents.sort((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()));

            log.info("Found {} events for resident: {} in date range",
                    allEvents.size(), residentId);

            return allEvents;

        } catch (Exception e) {
            log.error("Error fetching events for resident: {}", residentId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets events for a specific device in a date range.
     *
     * @param deviceId The device ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of EventDTO
     */
    public List<EventDTO> getEventsByDeviceId(
            Long deviceId,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("Fetching events for device: {} from {} to {}",
                deviceId, startDate, endDate);

        try {
            List<Event> events = eventQueryService.handle(
                    new GetAllEventsBySensorId(deviceId)
            );

            return events.stream()
                    .map(event -> toEventDTO(event, deviceId))
                    .filter(eventDTO -> isInDateRange(eventDTO, startDate, endDate))
                    .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching events for device: {}", deviceId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Transforms a Device entity to DeviceDTO.
     */
    private DeviceDTO toDeviceDTO(Device device) {
        return new DeviceDTO(
                device.getId(),
                device.getType(),
                device.getStatus(),
                device.getDescription(),
                device.getResidentId()
        );
    }

    /**
     * Transforms an Event entity to EventDTO.
     * Note: Event entity uses createdAt/updatedAt from AuditableAbstractAggregateRoot
     */
    private EventDTO toEventDTO(Event event, Long deviceId) {
        // Extract timestamp from createdAt (assuming events are created when measured)
        LocalDateTime timestamp = event.getCreatedAt()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return new EventDTO(
                event.getId(),
                event.getEventType(),
                event.getQualityValue(),
                event.getLevelValue(),
                deviceId,
                timestamp
        );
    }

    /**
     * Checks if an event falls within the date range.
     */
    private boolean isInDateRange(EventDTO event, LocalDate startDate, LocalDate endDate) {
        LocalDate eventDate = event.getDate();
        return !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate);
    }
}