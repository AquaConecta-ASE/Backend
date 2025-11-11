package com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.auth0.services;

import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class Auth0UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Auth0UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final ProviderRepository providerRepository;
    private final ResidentRepository residentRepository;

    public Auth0UserService(UserRepository userRepository, RoleRepository roleRepository, 
                           ProfileRepository profileRepository, ProviderRepository providerRepository,
                           ResidentRepository residentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.providerRepository = providerRepository;
        this.residentRepository = residentRepository;
    }

    @Transactional
    public User syncUserFromAuth0(Authentication authentication) {
        LOGGER.info("🔄 Iniciando sincronización de usuario desde Auth0...");
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String auth0Id = jwt.getSubject();
        
        // Intentar obtener email del claim personalizado primero, luego del claim estándar
        String email = jwt.getClaim("https://aquaconecta.com/email");
        if (email == null) {
            email = jwt.getClaim("email");
        }
        
        // Intentar obtener nickname del claim personalizado primero
        String nickname = jwt.getClaim("https://aquaconecta.com/nickname");
        if (nickname == null) {
            nickname = jwt.getClaim("nickname");
        }
        
        String username = extractUsername(jwt, nickname, email);
        List<String> rolesFromToken = jwt.getClaim("https://aquaconecta.com/roles");
        
        LOGGER.info("📋 Datos extraídos del JWT:");
        LOGGER.info("   - Auth0 ID: {}", auth0Id);
        LOGGER.info("   - Username: {}", username);
        LOGGER.info("   - Email: {}", email);
        LOGGER.info("   - Nickname: {}", nickname);
        LOGGER.info("   - Roles: {}", rolesFromToken);
        
        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
        User user;
        
        if (existingUser.isPresent()) {
            LOGGER.info("👤 Usuario existente encontrado en BD, actualizando roles...");
            user = existingUser.get();
            updateUserRoles(user, rolesFromToken);
        } else {
            LOGGER.info("✨ Usuario nuevo, creando en BD local...");
            user = createNewUser(auth0Id, username, email, rolesFromToken);
        }
        
        User savedUser = userRepository.save(user);
        LOGGER.info("💾 Usuario guardado en BD con ID: {}", savedUser.getId());
        
        // Crear Provider o Resident automáticamente según el rol (sin Profile completo)
        if (existingUser.isEmpty()) {
            try {
                createRoleSpecificEntity(savedUser, rolesFromToken);
            } catch (Exception e) {
                LOGGER.error("❌ Error creando Provider/Resident automático: {}", e.getMessage(), e);
                // Continuar sin fallar - el usuario puede completar su perfil manualmente
            }
        }
        
        return savedUser;
    }

    private String extractUsername(Jwt jwt, String nickname, String email) {
        // Prioridad: nickname > email (parte antes de @) > auth0Id
        if (nickname != null && !nickname.isEmpty()) {
            LOGGER.debug("📝 Username extraído de nickname: {}", nickname);
            return nickname;
        }
        if (email != null && !email.isEmpty()) {
            String emailUsername = email.split("@")[0];
            LOGGER.debug("📝 Username extraído de email: {}", emailUsername);
            return emailUsername;
        }
        String auth0Id = jwt.getSubject();
        LOGGER.warn("⚠️ Username usando auth0Id (no hay nickname ni email): {}", auth0Id);
        return auth0Id;
    }

    private User createNewUser(String auth0Id, String username, String email, List<String> rolesFromToken) {
        LOGGER.info("🆕 Creando nuevo usuario: {}", username);
        
        List<Role> roles = getRolesFromStringList(rolesFromToken);
        
        if (roles.isEmpty()) {
            LOGGER.warn("⚠️ No se encontraron roles válidos en el token, asignando ROLE_PROVIDER por defecto");
            Role defaultRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                    .orElseGet(() -> {
                        LOGGER.info("📝 Creando rol ROLE_PROVIDER en BD");
                        return roleRepository.save(new Role(Roles.ROLE_PROVIDER));
                    });
            roles.add(defaultRole);
        }
        
        LOGGER.info("✅ Usuario creado con {} rol(es)", roles.size());
        return new User(username, auth0Id, roles);
    }

    private void updateUserRoles(User user, List<String> rolesFromToken) {
        if (rolesFromToken == null || rolesFromToken.isEmpty()) return;
        List<Role> newRoles = getRolesFromStringList(rolesFromToken);
        user.getRoles().clear();
        newRoles.forEach(user::addRole);
    }

    private List<Role> getRolesFromStringList(List<String> roleStrings) {
        List<Role> roles = new ArrayList<>();
        if (roleStrings == null) return roles;
        for (String roleString : roleStrings) {
            try {
                Roles roleEnum = Roles.valueOf(roleString);
                Role role = roleRepository.findByName(roleEnum)
                        .orElseGet(() -> roleRepository.save(new Role(roleEnum)));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rol desconocido: {}", roleString);
            }
        }
        return roles;
    }
    
    /**
     * Crea la entidad específica según el rol del usuario (Provider o Resident)
     * NO crea el Profile - esto debe hacerse manualmente vía POST /api/v1/profiles
     */
    private void createRoleSpecificEntity(User user, List<String> rolesFromToken) {
        LOGGER.info("📝 Creando entidad específica para usuario ID: {} con roles: {}", user.getId(), rolesFromToken);
        
        if (rolesFromToken == null || rolesFromToken.isEmpty()) {
            LOGGER.warn("⚠️ No hay roles para crear entidad específica");
            return;
        }
        
        // Determinar si es Provider o Resident y crear la entidad correspondiente
        if (rolesFromToken.contains("ROLE_PROVIDER")) {
            createProviderEntity(user);
        } else if (rolesFromToken.contains("ROLE_RESIDENT")) {
            createResidentEntity(user);
        } else {
            LOGGER.info("ℹ️ Rol no requiere entidad específica (ADMIN u otro)");
        }
    }
    
    private void createProviderEntity(User user) {
        // Verificar si ya existe un provider para este usuario
        List<Provider> existingProviders = providerRepository.findByUserId(user.getId());
        if (!existingProviders.isEmpty()) {
            LOGGER.info("ℹ️ Provider ya existe para usuario ID: {}", user.getId());
            return;
        }
        
        // Crear Provider con datos por defecto (el usuario puede actualizarlos después)
        String defaultTaxName = user.getUsername() + " Company"; // Nombre por defecto
        String defaultRuc = "PENDIENTE"; // RUC pendiente de actualizar
        
        Provider provider = new Provider(defaultTaxName, defaultRuc, user.getId());
        Provider savedProvider = providerRepository.save(provider);
        LOGGER.info("✅ Provider creado con ID: {} (Datos por defecto - requiere actualización)", savedProvider.getId());
    }
    
    private void createResidentEntity(User user) {
        // Verificar si ya existe un resident para este usuario
        List<Resident> existingResidents = residentRepository.findByUserId(user.getId());
        if (!existingResidents.isEmpty()) {
            LOGGER.info("ℹ️ Resident ya existe para usuario ID: {}", user.getId());
            return;
        }
        
        // Crear Resident con datos por defecto
        // Como no tenemos un provider específico, usamos un provider por defecto (ID=1) o dejamos en null
        // El usuario puede asociarse a un provider más tarde
        String[] nameParts = user.getUsername().split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : user.getUsername();
        String lastName = nameParts.length > 1 ? nameParts[1] : "Apellido";
        
        // Buscar el primer provider disponible o usar null
        List<Provider> providers = providerRepository.findAll();
        Long providerId = null;
        if (!providers.isEmpty()) {
            providerId = providers.get(0).getId();
            LOGGER.info("📌 Asignando Resident al Provider ID: {}", providerId);
        } else {
            LOGGER.warn("⚠️ No hay providers disponibles. Resident se creará sin provider asignado.");
            // Necesitamos un providerId válido, así que no podemos crear el resident aún
            LOGGER.warn("⚠️ No se puede crear Resident sin un Provider. Debe crearse manualmente.");
            return;
        }
        
        Resident resident = new Resident(firstName, lastName, user.getId(), providerId);
        Resident savedResident = residentRepository.save(resident);
        LOGGER.info("✅ Resident creado con ID: {} (Datos por defecto - requiere actualización)", savedResident.getId());
    }
}

