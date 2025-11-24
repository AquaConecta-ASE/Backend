package com.ironcoders.aquaconectabackend.profiles.integration;

import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.application.internal.comandservices.ProviderCommandServiceImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Provider operations
 * Tests the complete flow from command to database with real Spring context
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "auth0.management.domain=test.auth0.com",
    "auth0.management.client-id=test-client-id",
    "auth0.management.client-secret=test-client-secret"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Provider Integration Tests")
class ProviderIntegrationTest {

    @Autowired
    private ProviderCommandServiceImpl providerCommandService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private User testUser;
    private Role providerRole;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        providerRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure ROLE_PROVIDER exists
        providerRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                .orElseGet(() -> roleRepository.save(new Role(Roles.ROLE_PROVIDER)));

        // Create test user
        testUser = new User();
        testUser.setUsername("testprovider");
        testUser.setPassword("encrypted-password");
        testUser = userRepository.save(testUser);
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Integration: Should create provider with profile and assign role")
    void testCreateProvider_CompleteFlow() {
        // Arrange
        CreateProviderCommand command = new CreateProviderCommand(
                "Integration Test Company SAC",
                "20999888777",
                "Integration",
                "Test",
                "integration@test.com",
                "Av. Integration 123",
                "99887766",
                "DNI",
                "+51999888777",
                testUser.getId()
        );

        // Act
        Optional<Provider> result = providerCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent());
        Provider savedProvider = result.get();

        // Verify provider was saved
        assertNotNull(savedProvider.getId());
        assertEquals("Integration Test Company SAC", savedProvider.getTaxName());
        assertEquals("20999888777", savedProvider.getRuc());

        // Verify provider in database
        Optional<Provider> providerFromDb = providerRepository.findById(savedProvider.getId());
        assertTrue(providerFromDb.isPresent());
        assertEquals(testUser.getId(), providerFromDb.get().getUserId());

        // Verify user has ROLE_PROVIDER
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(updatedUser.getRoles().stream()
                .anyMatch(role -> role.getName() == Roles.ROLE_PROVIDER));

        // Verify profile was created
        Optional<Profile> profile = profileRepository.findByUserId(testUser.getId());
        assertTrue(profile.isPresent());
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Integration: Should not duplicate role when creating multiple providers")
    void testCreateProvider_RoleNotDuplicated() {
        // Arrange
        CreateProviderCommand command = new CreateProviderCommand(
                "Test Company",
                "20123456789",
                "First",
                "Provider",
                "first@test.com",
                "Address 1",
                "12345678",
                "DNI",
                "+51123456789",
                testUser.getId()
        );

        // Act - Create provider twice
        providerCommandService.handle(command);
        
        User user = userRepository.findById(testUser.getId()).orElseThrow();
        long roleCountBefore = user.getRoles().stream()
                .filter(role -> role.getName() == Roles.ROLE_PROVIDER)
                .count();

        // Try to create again (simulating profile completion again)
        CreateProviderCommand secondCommand = new CreateProviderCommand(
                "Second Company",
                "20987654321",
                "Second",
                "Provider",
                "second@test.com",
                "Address 2",
                "87654321",
                "DNI",
                "+51987654321",
                testUser.getId()
        );
        providerCommandService.handle(secondCommand);

        // Assert - Role should not be duplicated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        long roleCountAfter = updatedUser.getRoles().stream()
                .filter(role -> role.getName() == Roles.ROLE_PROVIDER)
                .count();

        assertEquals(roleCountBefore, roleCountAfter);
        assertEquals(1L, roleCountAfter);
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Integration: Should update provider successfully")
    void testUpdateProvider_CompleteFlow() {
        // Arrange - Create provider first
        CreateProviderCommand createCommand = new CreateProviderCommand(
                "Original Company",
                "20111111111",
                "Original",
                "Name",
                "original@test.com",
                "Original Address",
                "11111111",
                "DNI",
                "+51111111111",
                testUser.getId()
        );
        providerCommandService.handle(createCommand);

        UpdateProviderCommand updateCommand = new UpdateProviderCommand(
                "Updated Company SAC",
                "20222222222",
                testUser.getId()
        );

        // Act
        Optional<Provider> result = providerCommandService.handle(updateCommand);

        // Assert
        assertTrue(result.isPresent());
        Provider updatedProvider = result.get();
        assertEquals("Updated Company SAC", updatedProvider.getTaxName());
        assertEquals("20222222222", updatedProvider.getRuc());

        // Verify in database
        List<Provider> providersFromDb = providerRepository.findByUserId(testUser.getId());
        assertFalse(providersFromDb.isEmpty());
        Provider providerFromDb = providersFromDb.get(0);
        assertEquals("Updated Company SAC", providerFromDb.getTaxName());
        assertEquals("20222222222", providerFromDb.getRuc());
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Should handle provider creation failure when user not found")
    void testCreateProvider_UserNotFound() {
        // Arrange
        CreateProviderCommand command = new CreateProviderCommand(
                "Test Company",
                "20123456789",
                "Test",
                "User",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                999L // Non-existent user ID
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            providerCommandService.handle(command)
        );

        // Verify no provider was created
        assertEquals(0, providerRepository.count());
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Integration: Should not create duplicate profiles")
    void testCreateProvider_ProfileNotDuplicated() {
        // Arrange - Create profile manually first
        Profile existingProfile = profileRepository.save(
            createTestProfile(testUser.getId())
        );

        CreateProviderCommand command = new CreateProviderCommand(
                "Test Company",
                "20123456789",
                "Test",
                "Provider",
                "test@test.com",
                "Address",
                "12345678",
                "DNI",
                "+51123456789",
                testUser.getId()
        );

        // Act
        providerCommandService.handle(command);

        // Assert - Should still have only one profile
        long profileCount = profileRepository.findAll().stream()
                .filter(p -> p.getUserId().equals(testUser.getId()))
                .count();
        assertEquals(1L, profileCount);
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Should handle update failure when provider not found")
    void testUpdateProvider_ProviderNotFound() {
        // Arrange
        UpdateProviderCommand command = new UpdateProviderCommand(
                "Updated Company",
                "20999999999",
                testUser.getId()
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            providerCommandService.handle(command)
        );
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("Integration: Should persist all provider data correctly")
    void testCreateProvider_DataPersistence() {
        // Arrange
        CreateProviderCommand command = new CreateProviderCommand(
                "Persistence Test Company",
                "20555666777",
                "Persistence",
                "Tester",
                "persist@test.com",
                "Av. Persistence 999",
                "55566677",
                "CE",
                "+51555666777",
                testUser.getId()
        );

        // Act
        Optional<Provider> result = providerCommandService.handle(command);
        assertTrue(result.isPresent());
        Long providerId = result.get().getId();

        // Clear persistence context to force fresh database read
        providerRepository.flush();

        // Assert - Read from database again
        Optional<Provider> freshProvider = providerRepository.findById(providerId);
        assertTrue(freshProvider.isPresent());
        assertEquals("Persistence Test Company", freshProvider.get().getTaxName());
        assertEquals("20555666777", freshProvider.get().getRuc());
        assertEquals(testUser.getId(), freshProvider.get().getUserId());

        // Verify profile data
        Optional<Profile> profile = profileRepository.findByUserId(testUser.getId());
        assertTrue(profile.isPresent());
    }

    private Profile createTestProfile(Long userId) {
        Profile profile = new Profile();
        // Assuming Profile has setters or a constructor - adjust as needed
        profile.setUserId(userId);
        return profile;
    }
}
