package com.ironcoders.aquaconectabackend.requests;

import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProviderContextFacade.ProviderContextFacade;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.requests.application.internal.commandservices.WaterSupplyRequestCommandServiceImpl;
import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.WaterSupplyRequest;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.requests.infrastructure.persistence.jpa.repositories.WaterSupplyRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WaterSupplyRequestCommandServiceImpl.
 * Tests water supply request creation and status updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Water Supply Request Command Service Tests")
class WaterSupplyRequestCommandServiceTest {

    @Mock
    private WaterSupplyRequestRepository waterSupplyRequestRepository;

    @Mock
    private ResidentContextFacade residentContextFacade;

    @Mock
    private ProviderContextFacade providerContextFacade;

    @InjectMocks
    private WaterSupplyRequestCommandServiceImpl waterSupplyRequestCommandService;

    private Long residentId;
    private Long providerId;
    private Float waterQuantity;
    private LocalDate scheduledDate;

    @BeforeEach
    void setUp() {
        residentId = 1L;
        providerId = 2L;
        waterQuantity = 5000.0f;
        scheduledDate = LocalDate.now().plusDays(2);
    }

    @Test
    @DisplayName("Should successfully create water supply request")
    void testCreateWaterSupplyRequest_Success() {
        // Arrange
        Resident mockResident = mock(Resident.class);
        when(mockResident.getId()).thenReturn(residentId);

        Provider mockProvider = mock(Provider.class);
        when(mockProvider.getId()).thenReturn(providerId);

        when(residentContextFacade.getResidentById(residentId)).thenReturn(Optional.of(mockResident));
        when(providerContextFacade.getProviderById(providerId)).thenReturn(Optional.of(mockProvider));

        CreateWaterSupplyRequestCommand command = new CreateWaterSupplyRequestCommand(
                residentId,
                providerId,
                waterQuantity,
                scheduledDate
        );

        WaterSupplyRequest expectedRequest = new WaterSupplyRequest(command);
        when(waterSupplyRequestRepository.save(any(WaterSupplyRequest.class))).thenReturn(expectedRequest);

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Water supply request should be created successfully");
        assertEquals(residentId, result.get().getResidentId(), "Resident ID should match");
        assertEquals(providerId, result.get().getProviderId(), "Provider ID should match");
        assertEquals(waterQuantity, result.get().getWaterQuantity(), "Water quantity should match");
        assertEquals(scheduledDate, result.get().getScheduledDate(), "Scheduled date should match");

        verify(residentContextFacade, times(1)).getResidentById(residentId);
        verify(providerContextFacade, times(1)).getProviderById(providerId);
        verify(waterSupplyRequestRepository, times(1)).save(any(WaterSupplyRequest.class));
    }

    @Test
    @DisplayName("Should fail to create request when resident not found")
    void testCreateWaterSupplyRequest_ResidentNotFound() {
        // Arrange
        when(residentContextFacade.getResidentById(residentId)).thenReturn(Optional.empty());

        CreateWaterSupplyRequestCommand command = new CreateWaterSupplyRequestCommand(
                residentId,
                providerId,
                waterQuantity,
                scheduledDate
        );

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Request should not be created when resident doesn't exist");

        verify(residentContextFacade, times(1)).getResidentById(residentId);
        verify(providerContextFacade, never()).getProviderById(anyLong());
        verify(waterSupplyRequestRepository, never()).save(any(WaterSupplyRequest.class));
    }

    @Test
    @DisplayName("Should fail to create request when provider not found")
    void testCreateWaterSupplyRequest_ProviderNotFound() {
        // Arrange
        Resident mockResident = mock(Resident.class);
        when(residentContextFacade.getResidentById(residentId)).thenReturn(Optional.of(mockResident));
        when(providerContextFacade.getProviderById(providerId)).thenReturn(Optional.empty());

        CreateWaterSupplyRequestCommand command = new CreateWaterSupplyRequestCommand(
                residentId,
                providerId,
                waterQuantity,
                scheduledDate
        );

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Request should not be created when provider doesn't exist");

        verify(residentContextFacade, times(1)).getResidentById(residentId);
        verify(providerContextFacade, times(1)).getProviderById(providerId);
        verify(waterSupplyRequestRepository, never()).save(any(WaterSupplyRequest.class));
    }

    @Test
    @DisplayName("Should successfully update request status to COMPLETED")
    void testUpdateWaterSupplyRequest_StatusToCompleted() {
        // Arrange
        Long requestId = 1L;
        String newStatus = "COMPLETED";
        LocalDateTime deliveryTime = LocalDateTime.now();

        WaterSupplyRequest existingRequest = mock(WaterSupplyRequest.class);
        when(existingRequest.getId()).thenReturn(requestId);
        when(existingRequest.getStatus()).thenReturn("PENDING");

        when(waterSupplyRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(waterSupplyRequestRepository.save(any(WaterSupplyRequest.class))).thenReturn(existingRequest);

        UpdateWaterSupplyRequestCommand command = new UpdateWaterSupplyRequestCommand(
                requestId,
                newStatus,
                deliveryTime
        );

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Request should be updated successfully");

        verify(waterSupplyRequestRepository, times(1)).findById(requestId);
        verify(existingRequest, times(1)).updateStatus(newStatus);
        verify(existingRequest, times(1)).setDeliveryTime(deliveryTime);
        verify(waterSupplyRequestRepository, times(1)).save(existingRequest);
    }

    @Test
    @DisplayName("Should create request with large water quantity")
    void testCreateWaterSupplyRequest_LargeQuantity() {
        // Arrange - 10,000 liters (full tank refill)
        Float largeQuantity = 10000.0f;

        Resident mockResident = mock(Resident.class);
        Provider mockProvider = mock(Provider.class);

        when(residentContextFacade.getResidentById(residentId)).thenReturn(Optional.of(mockResident));
        when(providerContextFacade.getProviderById(providerId)).thenReturn(Optional.of(mockProvider));

        CreateWaterSupplyRequestCommand command = new CreateWaterSupplyRequestCommand(
                residentId,
                providerId,
                largeQuantity,
                scheduledDate
        );

        WaterSupplyRequest expectedRequest = new WaterSupplyRequest(command);
        when(waterSupplyRequestRepository.save(any(WaterSupplyRequest.class))).thenReturn(expectedRequest);

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Large quantity request should be created successfully");
        assertEquals(largeQuantity, result.get().getWaterQuantity(), "Large quantity should be accepted");

        verify(waterSupplyRequestRepository, times(1)).save(any(WaterSupplyRequest.class));
    }

    @Test
    @DisplayName("Should create urgent request for same-day delivery")
    void testCreateWaterSupplyRequest_UrgentSameDayDelivery() {
        // Arrange - Same-day delivery (critical water shortage scenario)
        LocalDate sameDaySchedule = LocalDate.now();

        Resident mockResident = mock(Resident.class);
        Provider mockProvider = mock(Provider.class);

        when(residentContextFacade.getResidentById(residentId)).thenReturn(Optional.of(mockResident));
        when(providerContextFacade.getProviderById(providerId)).thenReturn(Optional.of(mockProvider));

        CreateWaterSupplyRequestCommand command = new CreateWaterSupplyRequestCommand(
                residentId,
                providerId,
                waterQuantity,
                sameDaySchedule
        );

        WaterSupplyRequest expectedRequest = new WaterSupplyRequest(command);
        when(waterSupplyRequestRepository.save(any(WaterSupplyRequest.class))).thenReturn(expectedRequest);

        // Act
        Optional<WaterSupplyRequest> result = waterSupplyRequestCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Urgent same-day request should be created");
        assertEquals(sameDaySchedule, result.get().getScheduledDate(), "Same-day delivery should be scheduled");

        verify(waterSupplyRequestRepository, times(1)).save(any(WaterSupplyRequest.class));
    }
}
