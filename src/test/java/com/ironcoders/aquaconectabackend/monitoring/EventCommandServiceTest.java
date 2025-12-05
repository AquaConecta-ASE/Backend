package com.ironcoders.aquaconectabackend.monitoring;

import com.ironcoders.aquaconectabackend.monitoring.application.internal.commandservices.EventCommandServiceImpl;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Event;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.monitoring.infrastructure.persistence.jpa.repositories.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventCommandServiceImpl.
 * Tests the creation of sensor events from IoT devices.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event Command Service Tests")
class EventCommandServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventCommandServiceImpl eventCommandService;

    private Long sensorId;
    private String eventType;
    private String qualityValue;
    private String levelValue;

    @BeforeEach
    void setUp() {
        sensorId = 1L;
        eventType = "measurement";
        qualityValue = "excellent";
        levelValue = "85";

        // Mock authentication context for IoT device
        UserDetails userDetails = User.builder()
                .username("iot-sensor-device-1")
                .password("sensor-password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SENSOR")))
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should successfully create event from sensor data")
    void testCreateEventFromSensorData_Success() {
        // Arrange
        CreateEventCommand command = new CreateEventCommand(
                eventType,
                qualityValue,
                levelValue,
                sensorId
        );

        Event expectedEvent = new Event(command);
        when(eventRepository.save(any(Event.class))).thenReturn(expectedEvent);

        // Act
        Optional<Event> result = eventCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Event should be created successfully");
        assertEquals(eventType, result.get().getEventType(), "Event type should match");
        assertEquals(qualityValue, result.get().getQualityValue(), "Quality value should match");
        assertEquals(levelValue, result.get().getLevelValue(), "Level value should match");
        assertEquals(sensorId, result.get().getSensorId(), "Sensor ID should match");

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event with critical water level")
    void testCreateEvent_CriticalWaterLevel() {
        // Arrange - Critical level below 40%
        String criticalLevel = "35";
        CreateEventCommand command = new CreateEventCommand(
                "alert",
                "poor",
                criticalLevel,
                sensorId
        );

        Event expectedEvent = new Event(command);
        when(eventRepository.save(any(Event.class))).thenReturn(expectedEvent);

        // Act
        Optional<Event> result = eventCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Event should be created even with critical level");
        assertEquals("alert", result.get().getEventType(), "Event type should be 'alert' for critical levels");
        assertEquals("poor", result.get().getQualityValue(), "Quality should be 'poor' for critical levels");
        assertEquals(criticalLevel, result.get().getLevelValue(), "Critical level should be recorded");

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should create event with excellent water quality")
    void testCreateEvent_ExcellentWaterQuality() {
        // Arrange - High quality water after refill
        CreateEventCommand command = new CreateEventCommand(
                "measurement",
                "excellent",
                "100",
                sensorId
        );

        Event expectedEvent = new Event(command);
        when(eventRepository.save(any(Event.class))).thenReturn(expectedEvent);

        // Act
        Optional<Event> result = eventCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Event should be created successfully");
        assertEquals("excellent", result.get().getQualityValue(), "Water quality should be excellent");
        assertEquals("100", result.get().getLevelValue(), "Level should be at maximum (100%)");

        verify(eventRepository, times(1)).save(any(Event.class));
    }
}
