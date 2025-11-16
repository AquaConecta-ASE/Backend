package com.ironcoders.aquaconectabackend.profiles.application.internal.comandservices;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.valueobjects.PersonName;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProviderCommandService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderCommandServiceImpl implements ProviderCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderCommandServiceImpl.class);

    private final ProviderRepository providerRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    @Value("${auth0.management.domain}")
    private String domain;
    
    @Value("${auth0.management.client-id:}")
    private String clientId;
    
    @Value("${auth0.management.client-secret:}")
    private String clientSecret;

    public ProviderCommandServiceImpl(
            ProviderRepository providerRepository, 
            ProfileRepository profileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository) {
        this.providerRepository = providerRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

   @Override
    public Optional<Provider> handle(CreateProviderCommand command) {
        LOGGER.info("🏢 Creando proveedor para usuario ID: {}", command.userId());
        
        // 1. Obtener usuario de la BD
        Optional<User> userOptional = userRepository.findById(command.userId());
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        User user = userOptional.get();
        
        // 2. Asignar rol ROLE_PROVIDER en BD local si no lo tiene
        boolean needsRoleUpdate = user.getRoles().stream()
                .noneMatch(role -> role.getName() == Roles.ROLE_PROVIDER);
        
        if (needsRoleUpdate) {
            LOGGER.info("📝 Asignando rol ROLE_PROVIDER al usuario en BD local");
            Role providerRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                    .orElseGet(() -> {
                        LOGGER.info("✨ Creando rol ROLE_PROVIDER en BD");
                        return roleRepository.save(new Role(Roles.ROLE_PROVIDER));
                    });
            user.addRole(providerRole);
            userRepository.save(user);
        }
        
        // 3. Actualizar Auth0 app_metadata con el rol
        if (user.getAuth0Id() != null && !user.getAuth0Id().isEmpty()) {
            try {
                updateAuth0RoleMetadata(user.getAuth0Id(), "ROLE_PROVIDER");
                LOGGER.info("✅ Rol ROLE_PROVIDER asignado en Auth0 app_metadata");
            } catch (Exception e) {
                LOGGER.error("❌ Error actualizando Auth0 app_metadata: {}", e.getMessage(), e);
                // Continuar - el usuario ya tiene el rol en BD local
            }
        } else {
            LOGGER.warn("⚠️ Usuario no tiene Auth0 ID, no se puede actualizar app_metadata");
        }
        
        // 4. Crear perfil si no existe
        if (profileRepository.findByUserId(command.userId()).isEmpty()) {
            PersonName name = new PersonName(command.firstName(), command.lastName());
            Profile profile = new Profile(
                    name,
                    command.email(),
                    command.direction(),
                    command.documentNumber(),
                    command.documentType(),
                    command.userId(),
                    command.phone()
            );
            profileRepository.save(profile);
            LOGGER.info("✅ Perfil creado para usuario ID: {}", command.userId());
        }

        // 5. Crear proveedor
        Provider provider = new Provider(command);
        providerRepository.save(provider);
        LOGGER.info("✅ Proveedor creado con ID: {}", provider.getId());

        return Optional.of(provider);
    }
    
    /**
     * Actualiza el app_metadata en Auth0 para asignar el rol al usuario
     */
    private void updateAuth0RoleMetadata(String auth0UserId, String role) throws Auth0Exception {
        LOGGER.info("🔐 Actualizando Auth0 app_metadata para usuario: {}", auth0UserId);
        
        // Obtener token de Management API
        var authAPI = com.auth0.client.auth.AuthAPI.newBuilder(domain, clientId, clientSecret).build();
        var tokenRequest = authAPI.requestToken("https://" + domain + "/api/v2/");
        var tokenHolder = tokenRequest.execute().getBody();
        String token = tokenHolder.getAccessToken();
        
        ManagementAPI mgmt = ManagementAPI.newBuilder(domain, token).build();
        
        // Actualizar app_metadata
        com.auth0.json.mgmt.users.User user = new com.auth0.json.mgmt.users.User();
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("role", role);
        user.setAppMetadata(appMetadata);
        
        mgmt.users().update(auth0UserId, user).execute();
        LOGGER.info("✅ app_metadata actualizado en Auth0");
    }

    @Override
    public Optional<Provider> handle(UpdateProviderCommand command) {

        Long userId = command.userId(); 

        List<Provider> existingProviders = providerRepository.findByUserId(userId);
        if (existingProviders.isEmpty()) {
            throw new IllegalArgumentException("No provider found for this user");
        }
        Provider provider = existingProviders.get(0);
        provider.update(command);
        providerRepository.save(provider);

        return Optional.of(provider);
    }
}
