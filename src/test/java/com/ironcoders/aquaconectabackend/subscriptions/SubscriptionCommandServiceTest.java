package com.ironcoders.aquaconectabackend.subscriptions;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl.DeviceContextFacade;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.application.internal.comandservices.SubscriptionCommandServiceImpl;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateAdditionalSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.UpdateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionCommandServiceImpl.
 * Tests subscription creation and update operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Command Service Tests")
class SubscriptionCommandServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ResidentContextFacade residentContextFacade;

    @Mock
    private DeviceContextFacade deviceContextFacade;

    @InjectMocks
    private SubscriptionCommandServiceImpl subscriptionCommandService;

    private Long residentId;
    private Long providerId;
    private Float waterTankSize;
    private Resident mockResident;
    private Device mockDevice;

    @BeforeEach
    void setUp() {
        residentId = 100L;
        providerId = 200L;
        waterTankSize = 1100.0f;

        // Create mock resident
        mockResident = new Resident();
        mockResident.setProviderId(providerId);

        // Create mock device
        mockDevice = new Device();
        mockDevice.setId(10L);
        mockDevice.setDeviceType("IOT");
        mockDevice.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should create additional subscription successfully")
    void shouldCreateAdditionalSubscriptionSuccessfully() {
        // Arrange
        CreateAdditionalSubscriptionCommand command = new CreateAdditionalSubscriptionCommand(
            residentId,
            waterTankSize
        );

        // Mock resident lookup
        when(residentContextFacade.findById(residentId))
            .thenReturn(Optional.of(mockResident));

        // Mock device creation
        when(deviceContextFacade.createDevice(any(CreateDeviceCommand.class)))
            .thenReturn(Optional.of(mockDevice));

        // Mock subscription save
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> {
                Subscription saved = invocation.getArgument(0);
                saved.setId(1L); // Simulate ID assignment
                return saved;
            });

        // Act
        Optional<Subscription> result = subscriptionCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Subscription should be created");
        Subscription subscription = result.get();
        
        assertEquals(residentId, subscription.getResidentId());
        assertEquals(providerId, subscription.getProviderId());
        assertEquals(mockDevice.getId(), subscription.getSensorId());
        assertEquals(waterTankSize, subscription.getWaterTankSize());
        assertEquals("ACTIVE", subscription.getStatus());
        assertNotNull(subscription.getStartDate());
        assertNotNull(subscription.getEndDate());
        assertTrue(subscription.getEndDate().isAfter(subscription.getStartDate()));

        // Verify interactions
        verify(residentContextFacade).findById(residentId);
        verify(deviceContextFacade).createDevice(argThat(cmd -> 
            "IOT".equals(cmd.deviceType()) && 
            "ACTIVE".equals(cmd.status()) &&
            "TDS/HC-SR04".equals(cmd.model())
        ));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should return empty when resident not found")
    void shouldReturnEmptyWhenResidentNotFound() {
        // Arrange
        CreateAdditionalSubscriptionCommand command = new CreateAdditionalSubscriptionCommand(
            residentId,
            waterTankSize
        );

        // Mock resident not found
        when(residentContextFacade.findById(residentId))
            .thenReturn(Optional.empty());

        // Act
        Optional<Subscription> result = subscriptionCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when resident not found");
        
        // Verify no device creation or subscription save
        verify(residentContextFacade).findById(residentId);
        verify(deviceContextFacade, never()).createDevice(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update subscription status and end date successfully")
    void shouldUpdateSubscriptionSuccessfully() {
        // Arrange
        Long subscriptionId = 1L;
        String newStatus = "INACTIVE";
        LocalDate newEndDate = LocalDate.now().plusMonths(2);

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId,
            newStatus,
            newEndDate
        );

        // Mock existing subscription
        Subscription existingSubscription = new Subscription(
            residentId,
            10L, // sensorId
            providerId,
            waterTankSize
        );
        existingSubscription.setId(subscriptionId);
        existingSubscription.setStatus("ACTIVE");

        when(subscriptionRepository.findById(subscriptionId))
            .thenReturn(Optional.of(existingSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Subscription> result = subscriptionCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Updated subscription should be returned");
        Subscription updated = result.get();
        
        assertEquals(subscriptionId, updated.getId());
        assertEquals(newStatus, updated.getStatus());
        assertEquals(newEndDate, updated.getEndDate());

        // Verify interactions
        verify(subscriptionRepository).findById(subscriptionId);
        verify(subscriptionRepository).save(existingSubscription);
    }

    @Test
    @DisplayName("Should return empty when updating non-existent subscription")
    void shouldReturnEmptyWhenUpdatingNonExistentSubscription() {
        // Arrange
        Long subscriptionId = 999L;
        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId,
            "INACTIVE",
            LocalDate.now().plusMonths(1)
        );

        // Mock subscription not found
        when(subscriptionRepository.findById(subscriptionId))
            .thenReturn(Optional.empty());

        // Act
        Optional<Subscription> result = subscriptionCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when subscription not found");
        
        // Verify no save operation
        verify(subscriptionRepository).findById(subscriptionId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle partial update with only status change")
    void shouldHandlePartialUpdateWithOnlyStatus() {
        // Arrange
        Long subscriptionId = 1L;
        String newStatus = "CANCELLED";

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId,
            newStatus,
            null // No end date change
        );

        Subscription existingSubscription = new Subscription(
            residentId,
            10L,
            providerId,
            waterTankSize
        );
        existingSubscription.setId(subscriptionId);
        existingSubscription.setStatus("ACTIVE");
        LocalDate originalEndDate = existingSubscription.getEndDate();

        when(subscriptionRepository.findById(subscriptionId))
            .thenReturn(Optional.of(existingSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<Subscription> result = subscriptionCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent());
        Subscription updated = result.get();
        
        assertEquals(newStatus, updated.getStatus(), "Status should be updated");
        assertEquals(originalEndDate, updated.getEndDate(), "End date should remain unchanged");

        verify(subscriptionRepository).save(existingSubscription);
    }
}
