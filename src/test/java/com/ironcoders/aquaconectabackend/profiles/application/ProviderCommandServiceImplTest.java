package com.ironcoders.aquaconectabackend.profiles.application;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProviderCommandServiceImpl
 * Tests the business logic for creating and updating providers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Provider Command Service Unit Tests")
class ProviderCommandServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private ProviderCommandServiceImpl providerCommandService;

    private User mockUser;
    private Role providerRole;
    private CreateProviderCommand createCommand;
    private UpdateProviderCommand updateCommand;

    @BeforeEach
    void setUp() {
        // Set up Auth0 configuration for testing
        ReflectionTestUtils.setField(providerCommandService, "domain", "test.auth0.com");
        ReflectionTestUtils.setField(providerCommandService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(providerCommandService, "clientSecret", "test-client-secret");

        // Create mock user with roles
        mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "username", "testprovider");
        ReflectionTestUtils.setField(mockUser, "auth0Id", "auth0|123456");
        mockUser.setRoles(new ArrayList<>());

        // Create provider role
        providerRole = new Role(Roles.ROLE_PROVIDER);
        ReflectionTestUtils.setField(providerRole, "id", 1L);

        // Create commands for testing
        createCommand = new CreateProviderCommand(
                "Test Company SAC",
                "20123456789",
                "John",
                "Doe",
                "john.doe@company.com",
                "Av. Test 123",
                "12345678",
                "DNI",
                "+51987654321",
                1L
        );

        updateCommand = new UpdateProviderCommand(
                "Updated Company SAC",
                "20987654321",
                1L
        );
    }

    @Test
    @DisplayName("Should create provider successfully when user exists")
    void testCreateProvider_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName(Roles.ROLE_PROVIDER)).thenReturn(Optional.of(providerRole));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert
        assertTrue(result.isPresent());
        Provider provider = result.get();
        assertEquals("Test Company SAC", provider.getTaxName());
        assertEquals("20123456789", provider.getRuc());
        assertEquals(1L, provider.getUserId());

        // Verify interactions
        verify(userRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).findByName(Roles.ROLE_PROVIDER);
        verify(userRepository, times(1)).save(mockUser);
        verify(profileRepository, times(1)).save(any(Profile.class));
        verify(providerRepository, times(1)).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testCreateProvider_UserNotFound() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> providerCommandService.handle(createCommand)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should not duplicate role when user already has ROLE_PROVIDER")
    void testCreateProvider_RoleAlreadyExists() {
        // Arrange
        mockUser.getRoles().add(providerRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert
        assertTrue(result.isPresent());
        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(mockUser); // Should not update user roles
    }

    @Test
    @DisplayName("Should create provider role if it doesn't exist in database")
    void testCreateProvider_CreateRoleIfNotExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName(Roles.ROLE_PROVIDER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(providerRole);
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert
        assertTrue(result.isPresent());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should not create profile when profile already exists")
    void testCreateProvider_ProfileAlreadyExists() {
        // Arrange
        Profile existingProfile = mock(Profile.class);
        mockUser.getRoles().add(providerRole);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(existingProfile));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert
        assertTrue(result.isPresent());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    @DisplayName("Should handle Auth0 update failure gracefully")
    void testCreateProvider_Auth0UpdateFailure() {
        // Arrange - User without Auth0 ID
        ReflectionTestUtils.setField(mockUser, "auth0Id", null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findByName(Roles.ROLE_PROVIDER)).thenReturn(Optional.of(providerRole));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArguments()[0]);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert - Should still succeed even without Auth0 update
        assertTrue(result.isPresent());
        verify(providerRepository, times(1)).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should update provider successfully")
    void testUpdateProvider_Success() {
        // Arrange
        Provider existingProvider = new Provider("Old Company", "20111111111", 1L);
        ReflectionTestUtils.setField(existingProvider, "id", 1L);

        when(providerRepository.findByUserId(1L)).thenReturn(List.of(existingProvider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Optional<Provider> result = providerCommandService.handle(updateCommand);

        // Assert
        assertTrue(result.isPresent());
        Provider updatedProvider = result.get();
        assertEquals("Updated Company SAC", updatedProvider.getTaxName());
        assertEquals("20987654321", updatedProvider.getRuc());
        verify(providerRepository, times(1)).save(existingProvider);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent provider")
    void testUpdateProvider_ProviderNotFound() {
        // Arrange
        when(providerRepository.findByUserId(anyLong())).thenReturn(List.of());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> providerCommandService.handle(updateCommand)
        );

        assertEquals("No provider found for this user", exception.getMessage());
        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("Should validate command fields are not null")
    void testCreateProviderCommand_ValidationNotNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            new CreateProviderCommand(null, "20123456789", "John", "Doe", 
                "email@test.com", "Address", "12345678", "DNI", "+51987654321", 1L)
        );
    }

    @Test
    @DisplayName("Should validate update command fields are not null")
    void testUpdateProviderCommand_ValidationNotNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            new UpdateProviderCommand(null, "20123456789", 1L)
        );
        
        assertThrows(NullPointerException.class, () -> 
            new UpdateProviderCommand("Company", null, 1L)
        );
        
        assertThrows(NullPointerException.class, () -> 
            new UpdateProviderCommand("Company", "20123456789", null)
        );
    }

    @Test
    @DisplayName("Should create provider with all command fields correctly mapped")
    void testCreateProvider_FieldMapping() {
        // Arrange
        mockUser.getRoles().add(providerRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mock(Profile.class)));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            ReflectionTestUtils.setField(provider, "id", 1L);
            return provider;
        });

        // Act
        Optional<Provider> result = providerCommandService.handle(createCommand);

        // Assert
        assertTrue(result.isPresent());
        Provider provider = result.get();
        assertAll(
            () -> assertEquals(createCommand.taxName(), provider.getTaxName()),
            () -> assertEquals(createCommand.ruc(), provider.getRuc()),
            () -> assertEquals(createCommand.userId(), provider.getUserId())
        );
    }
}
