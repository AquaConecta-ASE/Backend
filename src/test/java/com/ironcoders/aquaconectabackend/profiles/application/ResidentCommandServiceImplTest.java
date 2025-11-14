package com.ironcoders.aquaconectabackend.profiles.application;

import com.ironcoders.aquaconectabackend.iam.interfaces.acl.IamContextFacade;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl.DeviceContextFacade;
import com.ironcoders.aquaconectabackend.profiles.application.internal.comandservices.ResidentCommandServiceImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.dto.ResidentWithCredentials;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProfilesContextFacade.ProfilesContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.acl.SubscriptionContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResidentCommandServiceImpl
 * Tests the business logic for creating and updating residents
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resident Command Service Unit Tests")
class ResidentCommandServiceImplTest {

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private IamContextFacade iamContextFacade;

    @Mock
    private ProfilesContextFacade profilesContextFacade;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private DeviceContextFacade deviceContextFacade;

    @Mock
    private SubscriptionContextFacade subscriptionContextFacade;

    @InjectMocks
    private ResidentCommandServiceImpl residentCommandService;

    private Provider mockProvider;
    private Profile mockProfile;
    private Device mockDevice;
    private CreateResidentCommand createCommand;
    private UpdateResidentCommand updateCommand;

    @BeforeEach
    void setUp() {
        // Create mock provider
        mockProvider = new Provider("Test Company", "20123456789", 1L);
        ReflectionTestUtils.setField(mockProvider, "id", 1L);

        // Create mock profile
        mockProfile = mock(Profile.class);

        // Create mock device
        mockDevice = mock(Device.class);
        when(mockDevice.getId()).thenReturn(1L);

        // Create commands for testing
        createCommand = new CreateResidentCommand(
                "Jane",
                "Smith",
                "jane.smith@test.com",
                "Av. Resident 456",
                "87654321",
                "DNI",
                "+51912345678",
                1L,
                500.0f
        );

        updateCommand = new UpdateResidentCommand(
                "Jane Updated",
                "Smith Updated",
                1L
        );
    }

    @Test
    @DisplayName("Should create resident successfully with all dependencies")
    void testCreateResident_Success() throws AccessDeniedException {
        // Arrange
        Long expectedUserId = 10L;
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(expectedUserId);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));

        // Act
        ResidentWithCredentials result = residentCommandService.handle(createCommand);

        // Assert
        assertNotNull(result);
        assertNotNull(result.resident());
        assertEquals("Jane", result.resident().getFirstName());
        assertEquals("Smith", result.resident().getLastName());
        assertEquals(expectedUserId, result.resident().getUserId());
        assertEquals(1L, result.resident().getProviderId());
        assertEquals("Jane.Smith", result.username());
        assertEquals("87654321", result.password());

        // Verify all interactions
        verify(providerRepository, times(1)).findByUserId(1L);
        verify(profileRepository, times(1)).findByUserId(1L);
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                eq("Jane.Smith"),
                eq("87654321"),
                eq("jane.smith@test.com"),
                eq("Jane"),
                eq("Smith"),
                eq(List.of("ROLE_RESIDENT")),
                eq(1L)
        );
        verify(profilesContextFacade, times(1)).createProfileForResident(
                eq(expectedUserId),
                eq("Jane"),
                eq("Smith"),
                eq("jane.smith@test.com"),
                eq("Av. Resident 456"),
                eq("87654321"),
                eq("DNI"),
                eq("+51912345678")
        );
        verify(residentRepository, times(1)).save(any(Resident.class));
        verify(deviceContextFacade, times(1)).createDevice(any());
        verify(subscriptionContextFacade, times(1)).createSubscription(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when provider does not exist")
    void testCreateResident_ProviderNotFound() {
        // Arrange
        when(providerRepository.findByUserId(anyLong())).thenReturn(List.of());

        // Act & Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> residentCommandService.handle(createCommand)
        );

        assertEquals("Provider does not exist.", exception.getMessage());
        verify(residentRepository, never()).save(any(Resident.class));
    }

    @Test
    @DisplayName("Should throw exception when provider profile does not exist")
    void testCreateResident_ProviderProfileNotFound() {
        // Arrange
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> residentCommandService.handle(createCommand)
        );

        assertEquals("No profile found for this provider.", exception.getMessage());
        verify(residentRepository, never()).save(any(Resident.class));
    }

    @Test
    @DisplayName("Should throw exception when user creation fails")
    void testCreateResident_UserCreationFails() {
        // Arrange
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(0L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> residentCommandService.handle(createCommand)
        );

        assertEquals("Could not create resident user.", exception.getMessage());
        verify(residentRepository, never()).save(any(Resident.class));
    }

    @Test
    @DisplayName("Should create resident without subscription when device creation fails")
    void testCreateResident_DeviceCreationFails() throws AccessDeniedException {
        // Arrange
        Long expectedUserId = 10L;
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(expectedUserId);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.empty());

        // Act
        ResidentWithCredentials result = residentCommandService.handle(createCommand);

        // Assert
        assertNotNull(result);
        verify(subscriptionContextFacade, never()).createSubscription(any());
    }

    @Test
    @DisplayName("Should generate username correctly from first and last name")
    void testCreateResident_UsernameGeneration() throws AccessDeniedException {
        // Arrange
        CreateResidentCommand commandWithSpaces = new CreateResidentCommand(
                "Maria Jose",
                "Garcia Lopez",
                "maria@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51999999999",
                1L,
                300.0f
        );

        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(10L);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));

        // Act
        ResidentWithCredentials result = residentCommandService.handle(commandWithSpaces);

        // Assert
        assertEquals("Maria Jose.Garcia Lopez", result.username());
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                eq("Maria Jose.Garcia Lopez"),
                anyString(), anyString(), anyString(), anyString(), anyList(), anyLong()
        );
    }

    @Test
    @DisplayName("Should use document number as default password")
    void testCreateResident_DefaultPassword() throws AccessDeniedException {
        // Arrange
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(10L);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));

        // Act
        ResidentWithCredentials result = residentCommandService.handle(createCommand);

        // Assert
        assertEquals(createCommand.documentNumber(), result.password());
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                anyString(),
                eq("87654321"),
                anyString(), anyString(), anyString(), anyList(), anyLong()
        );
    }

    @Test
    @DisplayName("Should update resident successfully")
    void testUpdateResident_Success() {
        // Arrange
        Resident existingResident = new Resident("OldFirst", "OldLast", 1L, 1L);
        ReflectionTestUtils.setField(existingResident, "id", 1L);

        when(residentRepository.findByUserId(1L)).thenReturn(List.of(existingResident));
        when(residentRepository.save(any(Resident.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Optional<Resident> result = residentCommandService.handle(updateCommand);

        // Assert
        assertTrue(result.isPresent());
        Resident updatedResident = result.get();
        assertEquals("Jane Updated", updatedResident.getFirstName());
        assertEquals("Smith Updated", updatedResident.getLastName());
        verify(residentRepository, times(1)).save(existingResident);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent resident")
    void testUpdateResident_ResidentNotFound() {
        // Arrange
        when(residentRepository.findByUserId(anyLong())).thenReturn(List.of());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> residentCommandService.handle(updateCommand)
        );

        assertEquals("No resident found for this user", exception.getMessage());
        verify(residentRepository, never()).save(any(Resident.class));
    }

    @Test
    @DisplayName("Should validate create command fields are not null")
    void testCreateResidentCommand_ValidationNotNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            new CreateResidentCommand(null, "Smith", "email@test.com", 
                "Address", "12345678", "DNI", "+51999999999", 1L, 500.0f)
        );

        assertThrows(NullPointerException.class, () -> 
            new CreateResidentCommand("Jane", null, "email@test.com", 
                "Address", "12345678", "DNI", "+51999999999", 1L, 500.0f)
        );

        assertThrows(NullPointerException.class, () -> 
            new CreateResidentCommand("Jane", "Smith", null, 
                "Address", "12345678", "DNI", "+51999999999", 1L, 500.0f)
        );
    }

    @Test
    @DisplayName("Should validate update command fields are not null")
    void testUpdateResidentCommand_ValidationNotNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            new UpdateResidentCommand(null, "Smith", 1L)
        );

        assertThrows(NullPointerException.class, () -> 
            new UpdateResidentCommand("Jane", null, 1L)
        );

        assertThrows(NullPointerException.class, () -> 
            new UpdateResidentCommand("Jane", "Smith", null)
        );
    }

    @Test
    @DisplayName("Should pass water tank size to subscription creation")
    void testCreateResident_WaterTankSizePassedToSubscription() throws AccessDeniedException {
        // Arrange
        Float waterTankSize = 750.5f;
        CreateResidentCommand commandWithTankSize = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51999999999",
                1L,
                waterTankSize
        );

        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(10L);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));

        // Act
        residentCommandService.handle(commandWithTankSize);

        // Assert
        verify(subscriptionContextFacade, times(1)).createSubscription(argThat(cmd ->
                cmd.waterTankSize().equals(waterTankSize)
        ));
    }

    @Test
    @DisplayName("Should create resident with correct field mapping")
    void testCreateResident_FieldMapping() throws AccessDeniedException {
        // Arrange
        when(providerRepository.findByUserId(1L)).thenReturn(List.of(mockProvider));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(iamContextFacade.createUserWithAuth0(
                anyString(), anyString(), anyString(), anyString(), 
                anyString(), anyList(), anyLong()
        )).thenReturn(10L);
        when(residentRepository.save(any(Resident.class))).thenAnswer(invocation -> {
            Resident resident = invocation.getArgument(0);
            ReflectionTestUtils.setField(resident, "id", 1L);
            return resident;
        });
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));

        // Act
        ResidentWithCredentials result = residentCommandService.handle(createCommand);

        // Assert
        Resident resident = result.resident();
        assertAll(
            () -> assertEquals(createCommand.firstName(), resident.getFirstName()),
            () -> assertEquals(createCommand.lastName(), resident.getLastName()),
            () -> assertEquals(10L, resident.getUserId()),
            () -> assertEquals(createCommand.providerId(), resident.getProviderId())
        );
    }
}
