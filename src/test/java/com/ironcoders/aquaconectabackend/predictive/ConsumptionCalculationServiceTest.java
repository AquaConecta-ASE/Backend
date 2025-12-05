package com.ironcoders.aquaconectabackend.predictive;

import com.ironcoders.aquaconectabackend.predictive.application.internal.ConsumptionCalculationServiceImpl;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.WaterConsumptionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.MonitoringContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.SubscriptionContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto.EventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsumptionCalculationServiceImpl.
 * Tests the calculation of daily water consumption from monitoring events.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consumption Calculation Service Tests")
class ConsumptionCalculationServiceTest {

    @Mock
    private WaterConsumptionRepository waterConsumptionRepository;

    @Mock
    private MonitoringContextFacade monitoringContextFacade;

    @Mock
    private SubscriptionContextFacade subscriptionContextFacade;

    @InjectMocks
    private ConsumptionCalculationServiceImpl consumptionCalculationService;

    private Long subscriptionId;
    private Long residentId;
    private Long sensorId;
    private Double waterTankSize;

    @BeforeEach
    void setUp() {
        subscriptionId = 1L;
        residentId = 100L;
        sensorId = 10L;
        waterTankSize = 1000.0; // 1000 liters tank
    }

    @Test
    @DisplayName("Should calculate daily consumption successfully with valid events")
    void shouldCalculateDailyConsumptionSuccessfully() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 11, 14);
        LocalDate endDate = LocalDate.of(2024, 11, 16);
        
        CalculateDailyConsumptionCommand command = new CalculateDailyConsumptionCommand(
            subscriptionId, residentId, startDate, endDate
        );

        // Mock subscription validation
        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(true);
        when(subscriptionContextFacade.getSensorIdBySubscription(subscriptionId))
            .thenReturn(Optional.of(sensorId));
        when(subscriptionContextFacade.getWaterTankSizeBySubscription(subscriptionId))
            .thenReturn(Optional.of(waterTankSize));

        // Mock monitoring events (percentage-based levels)
        List<EventDTO> mockEvents = Arrays.asList(
            createEvent(1L, LocalDateTime.of(2024, 11, 14, 8, 0), "level", 90.0),
            createEvent(2L, LocalDateTime.of(2024, 11, 14, 20, 0), "level", 75.0), // -15%
            createEvent(3L, LocalDateTime.of(2024, 11, 15, 8, 0), "level", 60.0),  // -15%
            createEvent(4L, LocalDateTime.of(2024, 11, 15, 20, 0), "level", 50.0), // -10%
            createEvent(5L, LocalDateTime.of(2024, 11, 16, 8, 0), "level", 35.0)   // -15%
        );

        when(monitoringContextFacade.getEventsBySubscriptionId(eq(subscriptionId), any(), any()))
            .thenReturn(mockEvents);

        // Mock repository save
        when(waterConsumptionRepository.save(any(WaterConsumption.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<WaterConsumption> result = consumptionCalculationService.handle(command);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 2, "Should have at least 2 days of consumption");
        
        // Verify consumption values (percentage converted to liters)
        // Day 1 (Nov 14): 90% -> 75% = -15% = -150 liters
        WaterConsumption day1 = result.stream()
            .filter(c -> c.getConsumptionDate().equals(LocalDate.of(2024, 11, 14)))
            .findFirst()
            .orElse(null);
        
        assertNotNull(day1);
        assertEquals(subscriptionId, day1.getSubscriptionId());
        assertTrue(day1.getConsumptionLiters() > 0, "Consumption should be positive");
        assertEquals(900.0, day1.getStartLevel(), 0.1); // 90% of 1000L
        assertEquals(750.0, day1.getEndLevel(), 0.1);   // 75% of 1000L
        assertFalse(day1.isRefillDay()); // No refill (decrease only)

        // Verify repository interactions
        verify(waterConsumptionRepository, atLeastOnce()).save(any(WaterConsumption.class));
        verify(subscriptionContextFacade).isSubscriptionOwnedByResident(subscriptionId, residentId);
        verify(subscriptionContextFacade).getSensorIdBySubscription(subscriptionId);
        verify(monitoringContextFacade).getEventsBySubscriptionId(eq(subscriptionId), any(), any());
    }

    @Test
    @DisplayName("Should return empty list when subscription does not belong to resident")
    void shouldReturnEmptyListWhenSubscriptionNotOwned() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 11, 14);
        LocalDate endDate = LocalDate.of(2024, 11, 16);
        
        CalculateDailyConsumptionCommand command = new CalculateDailyConsumptionCommand(
            subscriptionId, residentId, startDate, endDate
        );

        // Mock subscription validation to return false
        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(false);

        // Act
        List<WaterConsumption> result = consumptionCalculationService.handle(command);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list for unauthorized access");
        
        // Verify no further processing occurred
        verify(subscriptionContextFacade).isSubscriptionOwnedByResident(subscriptionId, residentId);
        verify(subscriptionContextFacade, never()).getSensorIdBySubscription(anyLong());
        verify(monitoringContextFacade, never()).getEventsBySubscriptionId(anyLong(), any(), any());
        verify(waterConsumptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should detect refill when water level increases significantly")
    void shouldDetectRefillWhenLevelIncreases() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 11, 14);
        LocalDate endDate = LocalDate.of(2024, 11, 15);
        
        CalculateDailyConsumptionCommand command = new CalculateDailyConsumptionCommand(
            subscriptionId, residentId, startDate, endDate
        );

        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(true);
        when(subscriptionContextFacade.getSensorIdBySubscription(subscriptionId))
            .thenReturn(Optional.of(sensorId));
        when(subscriptionContextFacade.getWaterTankSizeBySubscription(subscriptionId))
            .thenReturn(Optional.of(waterTankSize));

        // Mock events with REFILL (significant increase > 30%)
        List<EventDTO> mockEvents = Arrays.asList(
            createEvent(1L, LocalDateTime.of(2024, 11, 14, 8, 0), "level", 25.0),
            createEvent(2L, LocalDateTime.of(2024, 11, 14, 12, 0), "level", 95.0), // REFILL! +70%
            createEvent(3L, LocalDateTime.of(2024, 11, 14, 20, 0), "level", 85.0)
        );

        when(monitoringContextFacade.getEventsBySubscriptionId(eq(subscriptionId), any(), any()))
            .thenReturn(mockEvents);
        when(waterConsumptionRepository.save(any(WaterConsumption.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<WaterConsumption> result = consumptionCalculationService.handle(command);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify refill detection
        WaterConsumption day = result.get(0);
        assertTrue(day.isRefillDay(), "Should detect refill day when level increases >30%");
        
        verify(waterConsumptionRepository, atLeastOnce()).save(any(WaterConsumption.class));
    }

    // Helper method to create EventDTO
    private EventDTO createEvent(Long id, LocalDateTime timestamp, String type, Double value) {
        EventDTO event = new EventDTO();
        event.setId(id);
        event.setTimestamp(timestamp);
        event.setEventType(type);
        event.setLevelValue(value);
        return event;
    }
}
