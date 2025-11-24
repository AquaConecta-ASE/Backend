package com.ironcoders.aquaconectabackend.profiles.integration;

import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Resident operations
 * Uses real Spring context with some mocked external dependencies (Auth0, Device, Subscription)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Resident Integration Tests")
class ResidentIntegrationTest {

    @Autowired
    private ResidentCommandServiceImpl residentCommandService;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private IamContextFacade iamContextFacade;

    @MockBean
    private ProfilesContextFacade profilesContextFacade;

    @MockBean
    private DeviceContextFacade deviceContextFacade;

    @MockBean
    private SubscriptionContextFacade subscriptionContextFacade;

    private User providerUser;
    private Provider testProvider;
    private Profile providerProfile;
    private Device mockDevice;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        residentRepository.deleteAll();
        providerRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles exist
        Role providerRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                .orElseGet(() -> roleRepository.save(new Role(Roles.ROLE_PROVIDER)));

        // Create provider user
        providerUser = new User();
        providerUser.setUsername("testprovider");
        providerUser.setPassword("encrypted-password");
        providerUser = userRepository.save(providerUser);

        // Create provider
        testProvider = new Provider("Test Provider Company", "20123456789", providerUser.getId());
        testProvider = providerRepository.save(testProvider);

        // Create provider profile
        providerProfile = createTestProfile(providerUser.getId());
        providerProfile = profileRepository.save(providerProfile);

        // Mock device
        mockDevice = mock(Device.class);
        when(mockDevice.getId()).thenReturn(1L);

        // Setup default mocks
        when(iamContextFacade.createUserWithAuth0(
            anyString(), anyString(), anyString(), anyString(), 
            anyString(), anyList(), anyLong()
        )).thenReturn(100L);

        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.of(mockDevice));
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Integration: Should create resident with all dependencies")
    void testCreateResident_CompleteFlow() throws AccessDeniedException {
        // Arrange
        CreateResidentCommand command = new CreateResidentCommand(
                "Integration",
                "Resident",
                "integration.resident@test.com",
                "Av. Integration 123",
                "88776655",
                "DNI",
                "+51888777666",
                testProvider.getId(),
                600.0f
        );

        // Act
        ResidentWithCredentials result = residentCommandService.handle(command);

        // Assert
        assertNotNull(result);
        assertNotNull(result.resident());
        assertEquals("Integration", result.resident().getFirstName());
        assertEquals("Resident", result.resident().getLastName());
        assertEquals("Integration.Resident", result.username());
        assertEquals("88776655", result.password());

        // Verify resident was saved in database
        Optional<Resident> residentFromDb = residentRepository.findById(result.resident().getId());
        assertTrue(residentFromDb.isPresent());
        assertEquals(100L, residentFromDb.get().getUserId());
        assertEquals(testProvider.getId(), residentFromDb.get().getProviderId());

        // Verify interactions with facades
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                eq("Integration.Resident"),
                eq("88776655"),
                eq("integration.resident@test.com"),
                eq("Integration"),
                eq("Resident"),
                eq(List.of("ROLE_RESIDENT")),
                eq(testProvider.getId())
        );

        verify(profilesContextFacade, times(1)).createProfileForResident(
                eq(100L),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        );

        verify(deviceContextFacade, times(1)).createDevice(any());
        verify(subscriptionContextFacade, times(1)).createSubscription(any());
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Should fail when provider does not exist")
    void testCreateResident_ProviderNotFound() {
        // Arrange
        CreateResidentCommand command = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                999L, // Non-existent provider
                500.0f
        );

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> 
            residentCommandService.handle(command)
        );

        // Verify no resident was created
        assertEquals(0, residentRepository.count());
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Should fail when provider profile does not exist")
    void testCreateResident_ProviderProfileNotFound() {
        // Arrange - Delete provider profile
        profileRepository.delete(providerProfile);

        CreateResidentCommand command = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                testProvider.getId(),
                500.0f
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            residentCommandService.handle(command)
        );

        // Verify no resident was created
        assertEquals(0, residentRepository.count());
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Should fail when user creation fails")
    void testCreateResident_UserCreationFails() {
        // Arrange
        when(iamContextFacade.createUserWithAuth0(
            anyString(), anyString(), anyString(), anyString(), 
            anyString(), anyList(), anyLong()
        )).thenReturn(0L);

        CreateResidentCommand command = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                testProvider.getId(),
                500.0f
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            residentCommandService.handle(command)
        );

        // Verify no resident was created
        assertEquals(0, residentRepository.count());
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Integration: Should create resident without subscription when device creation fails")
    void testCreateResident_DeviceCreationFails() throws AccessDeniedException {
        // Arrange
        when(deviceContextFacade.createDevice(any())).thenReturn(Optional.empty());

        CreateResidentCommand command = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                testProvider.getId(),
                500.0f
        );

        // Act
        ResidentWithCredentials result = residentCommandService.handle(command);

        // Assert
        assertNotNull(result);
        verify(subscriptionContextFacade, never()).createSubscription(any());
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("Integration: Should update resident successfully")
    void testUpdateResident_CompleteFlow() throws AccessDeniedException {
        // Arrange - Create resident first
        CreateResidentCommand createCommand = new CreateResidentCommand(
                "Original",
                "Name",
                "original@test.com",
                "Original Address",
                "11111111",
                "DNI",
                "+51111111111",
                testProvider.getId(),
                500.0f
        );
        ResidentWithCredentials created = residentCommandService.handle(createCommand);

        UpdateResidentCommand updateCommand = new UpdateResidentCommand(
                "Updated",
                "Name",
                created.resident().getUserId()
        );

        // Act
        Optional<Resident> result = residentCommandService.handle(updateCommand);

        // Assert
        assertTrue(result.isPresent());
        Resident updatedResident = result.get();
        assertEquals("Updated", updatedResident.getFirstName());
        assertEquals("Name", updatedResident.getLastName());

        // Verify in database
        Optional<Resident> residentFromDb = residentRepository.findById(updatedResident.getId());
        assertTrue(residentFromDb.isPresent());
        assertEquals("Updated", residentFromDb.get().getFirstName());
    }

    @Test
    @Order(7)
    @DisplayName("Integration: Should fail update when resident not found")
    void testUpdateResident_ResidentNotFound() {
        // Arrange
        UpdateResidentCommand command = new UpdateResidentCommand(
                "Updated",
                "Name",
                999L // Non-existent user
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            residentCommandService.handle(command)
        );
    }

    @Test
    @Order(8)
    @Transactional
    @DisplayName("Integration: Should generate correct username from names")
    void testCreateResident_UsernameGeneration() throws AccessDeniedException {
        // Arrange
        CreateResidentCommand command = new CreateResidentCommand(
                "Maria Jose",
                "Garcia Lopez",
                "maria@test.com",
                "Address",
                "99887766",
                "DNI",
                "+51999888777",
                testProvider.getId(),
                400.0f
        );

        // Act
        ResidentWithCredentials result = residentCommandService.handle(command);

        // Assert
        assertEquals("Maria Jose.Garcia Lopez", result.username());
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                eq("Maria Jose.Garcia Lopez"),
                anyString(), anyString(), anyString(), anyString(), anyList(), anyLong()
        );
    }

    @Test
    @Order(9)
    @Transactional
    @DisplayName("Integration: Should use document number as password")
    void testCreateResident_PasswordFromDocument() throws AccessDeniedException {
        // Arrange
        String documentNumber = "12348765";
        CreateResidentCommand command = new CreateResidentCommand(
                "Test",
                "Resident",
                "test@test.com",
                "Address",
                documentNumber,
                "DNI",
                "+51123456789",
                testProvider.getId(),
                500.0f
        );

        // Act
        ResidentWithCredentials result = residentCommandService.handle(command);

        // Assert
        assertEquals(documentNumber, result.password());
        verify(iamContextFacade, times(1)).createUserWithAuth0(
                anyString(),
                eq(documentNumber),
                anyString(), anyString(), anyString(), anyList(), anyLong()
        );
    }

    @Test
    @Order(10)
    @Transactional
    @DisplayName("Integration: Should persist all resident data correctly")
    void testCreateResident_DataPersistence() throws AccessDeniedException {
        // Arrange
        CreateResidentCommand command = new CreateResidentCommand(
                "Persistence",
                "Test",
                "persist@test.com",
                "Av. Persistence 999",
                "55566677",
                "CE",
                "+51555666777",
                testProvider.getId(),
                750.5f
        );

        // Act
        ResidentWithCredentials result = residentCommandService.handle(command);
        Long residentId = result.resident().getId();

        // Clear persistence context to force fresh database read
        residentRepository.flush();

        // Assert - Read from database again
        Optional<Resident> freshResident = residentRepository.findById(residentId);
        assertTrue(freshResident.isPresent());
        assertEquals("Persistence", freshResident.get().getFirstName());
        assertEquals("Test", freshResident.get().getLastName());
        assertEquals(100L, freshResident.get().getUserId());
        assertEquals(testProvider.getId(), freshResident.get().getProviderId());
    }

    @Test
    @Order(11)
    @Transactional
    @DisplayName("Integration: Should pass water tank size to subscription")
    void testCreateResident_WaterTankSizePassedCorrectly() throws AccessDeniedException {
        // Arrange
        Float waterTankSize = 850.75f;
        CreateResidentCommand command = new CreateResidentCommand(
                "Tank",
                "Test",
                "tank@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                testProvider.getId(),
                waterTankSize
        );

        // Act
        residentCommandService.handle(command);

        // Assert
        verify(subscriptionContextFacade, times(1)).createSubscription(argThat(cmd ->
                cmd.waterTankSize().equals(waterTankSize)
        ));
    }

    private Profile createTestProfile(Long userId) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        return profile;
    }
}
