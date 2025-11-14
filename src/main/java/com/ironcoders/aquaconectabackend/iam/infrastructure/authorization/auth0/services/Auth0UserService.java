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
import com.ironcoders.aquaconectabackend.shared.Infrastructure.auth0.Auth0ManagementService;
import com.auth0.exception.Auth0Exception;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class Auth0UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Auth0UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final ProviderRepository providerRepository;
    private final ResidentRepository residentRepository;
    private final Auth0ManagementService auth0ManagementService;

    public Auth0UserService(UserRepository userRepository, RoleRepository roleRepository, 
                           ProfileRepository profileRepository, ProviderRepository providerRepository,
                           ResidentRepository residentRepository, Auth0ManagementService auth0ManagementService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.providerRepository = providerRepository;
        this.residentRepository = residentRepository;
        this.auth0ManagementService = auth0ManagementService;
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
        
        // Si no hay roles en el JWT, consultar Auth0 Management API para obtenerlos del app_metadata
        if (rolesFromToken == null || rolesFromToken.isEmpty()) {
            LOGGER.warn("⚠️ No se encontraron roles en el JWT, consultando Auth0 Management API...");
            try {
                rolesFromToken = getRolesFromAuth0Metadata(auth0Id);
                LOGGER.info("✅ Roles obtenidos de Auth0 app_metadata: {}", rolesFromToken);
            } catch (Exception e) {
                LOGGER.error("❌ Error obteniendo roles de Auth0: {}", e.getMessage());
                rolesFromToken = new ArrayList<>();
            }
        }
        
        LOGGER.info("📋 Datos extraídos del JWT:");
        LOGGER.info("   - Auth0 ID: {}", auth0Id);
        LOGGER.info("   - Username: {}", username);
        LOGGER.info("   - Email: {}", email);
        LOGGER.info("   - Nickname: {}", nickname);
        LOGGER.info("   - Roles: {}", rolesFromToken);
        
        // Buscar por auth0Id primero
        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
        
        // Si no existe por auth0Id, buscar por username (para evitar duplicados por race condition)
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByUsername(username);
            if (existingUser.isPresent()) {
                // Usuario existe con mismo username pero diferente auth0Id
                // Actualizar el auth0Id
                LOGGER.warn("⚠️ Usuario encontrado por username pero con auth0Id diferente. Actualizando...");
                User user = existingUser.get();
                user.setAuth0Id(auth0Id);
                updateUserRoles(user, rolesFromToken);
                userRepository.save(user);
            }
        }
        
        User user;
        boolean isNewUser = existingUser.isEmpty();
        
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
        
        // SIEMPRE intentar crear/vincular Provider o Resident según el rol
        // Esto es idempotente - solo creará/vinculará si no existe
        try {
            // Usar los roles de la BD (ya actualizados) en lugar de rolesFromToken
            createRoleSpecificEntity(savedUser);
        } catch (Exception e) {
            LOGGER.error("❌ Error creando/vinculando Provider/Resident: {}", e.getMessage(), e);
            // Continuar sin fallar - el usuario puede completar su perfil manualmente
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
    
    /**
     * Obtiene los roles del usuario desde Auth0 app_metadata
     * Usado cuando el JWT no contiene los roles en los claims
     */
    private List<String> getRolesFromAuth0Metadata(String auth0UserId) throws Auth0Exception {
        LOGGER.info("🔍 Consultando roles en Auth0 app_metadata para usuario: {}", auth0UserId);
        
        // Obtener usuario completo de Auth0
        com.auth0.json.mgmt.users.User auth0User = auth0ManagementService.getUser(auth0UserId);
        
        // Extraer app_metadata
        Map<String, Object> appMetadata = auth0User.getAppMetadata();
        
        if (appMetadata == null || appMetadata.isEmpty()) {
            LOGGER.warn("⚠️ No hay app_metadata para este usuario");
            return new ArrayList<>();
        }
        
        LOGGER.debug("📋 app_metadata: {}", appMetadata);
        
        // Extraer rol del app_metadata
        List<String> roles = new ArrayList<>();
        Object roleValue = appMetadata.get("role");
        
        if (roleValue instanceof String) {
            String role = (String) roleValue;
            LOGGER.info("✅ Rol encontrado en app_metadata: {}", role);
            roles.add(role);
        } else if (roleValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roleList = (List<String>) roleValue;
            roles.addAll(roleList);
            LOGGER.info("✅ Roles encontrados en app_metadata: {}", roles);
        } else {
            LOGGER.warn("⚠️ No se encontró rol en app_metadata (key: 'role')");
        }
        
        return roles;
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
        
        // Limpiar roles actuales solo si son diferentes
        List<Role> currentRoles = new ArrayList<>(user.getRoles());
        
        // Verificar si ya tiene exactamente los mismos roles
        if (currentRoles.size() == newRoles.size() && 
            currentRoles.containsAll(newRoles)) {
            LOGGER.debug("ℹ️ Roles sin cambios, omitiendo actualización");
            return;
        }
        
        // Solo actualizar si hay cambios
        user.getRoles().clear();
        newRoles.forEach(user::addRole);
        LOGGER.info("✅ Roles actualizados: {} roles", newRoles.size());
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
     * Usa los roles almacenados en la BD del usuario
     */
    private void createRoleSpecificEntity(User user) {
        // Obtener roles desde la BD del usuario (ya actualizados)
        List<String> userRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();
        
        LOGGER.info("📝 Creando entidad específica para usuario ID: {} con roles: {}", user.getId(), userRoles);
        
        if (userRoles.isEmpty()) {
            LOGGER.warn("⚠️ No hay roles para crear entidad específica");
            return;
        }
        
        // Determinar si es Provider o Resident y crear la entidad correspondiente
        if (userRoles.contains("ROLE_PROVIDER")) {
            createProviderEntity(user);
        } else if (userRoles.contains("ROLE_RESIDENT")) {
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
        // Para RESIDENTS: NO creamos automáticamente, solo vinculamos si ya existe
        // El Resident debe ser creado previamente por el Provider vía POST /residents/complete
        
        LOGGER.info("🔍 Buscando Resident pre-creado para vincular con User ID: {}", user.getId());
        
        // Ya existe un resident vinculado a este usuario?
        List<Resident> existingResidents = residentRepository.findByUserId(user.getId());
        if (!existingResidents.isEmpty()) {
            LOGGER.info("ℹ️ Resident ya vinculado para usuario ID: {}", user.getId());
            return;
        }
        
        // Obtener email del JWT (más confiable que username)
        // El email debe estar en el Profile que fue creado por el Provider
        
        // Buscar todos los residents sin userId asignado
        List<Resident> allResidents = residentRepository.findAll();
        List<Resident> unlinkedResidents = allResidents.stream()
                .filter(r -> r.getUserId() == null)
                .toList();
        
        if (unlinkedResidents.isEmpty()) {
            LOGGER.warn("⚠️ No hay Residents sin vincular disponibles");
            return;
        }
        
        LOGGER.info("📋 Encontrados {} residents sin vincular", unlinkedResidents.size());
        
        // Para cada resident sin vincular, buscar su profile y comparar por firstName + lastName
        for (Resident resident : unlinkedResidents) {
            LOGGER.debug("🔍 Evaluando Resident ID: {} ({} {})", 
                    resident.getId(), resident.getFirstName(), resident.getLastName());
            
            // Buscar profiles sin userId que coincidan con el nombre del resident
            List<Profile> allProfiles = profileRepository.findAll();
            Optional<Profile> matchingProfile = allProfiles.stream()
                    .filter(p -> p.getUserId() == null)
                    .filter(p -> p.getFirstName().equals(resident.getFirstName()) 
                              && p.getLastName().equals(resident.getLastName()))
                    .findFirst();
            
            if (matchingProfile.isPresent()) {
                Profile profile = matchingProfile.get();
                
                // Vincular resident con el usuario
                resident.setUserId(user.getId());
                residentRepository.save(resident);
                
                // También vincular el profile
                profile.setUserId(user.getId());
                profileRepository.save(profile);
                
                LOGGER.info("✅ VINCULACIÓN EXITOSA:");
                LOGGER.info("   - Resident ID: {} ({} {})", 
                        resident.getId(), resident.getFirstName(), resident.getLastName());
                LOGGER.info("   - Profile ID: {} (Email: {})", profile.getId(), profile.getEmail());
                LOGGER.info("   - User ID: {} (Auth0: {})", user.getId(), user.getAuth0Id());
                return;
            }
        }
        
        // Si llegamos aquí, no encontramos coincidencia por nombre
        // Como último recurso, vincular el primer resident sin userId
        if (!unlinkedResidents.isEmpty()) {
            Resident resident = unlinkedResidents.get(0);
            resident.setUserId(user.getId());
            residentRepository.save(resident);
            
            // Buscar y vincular también su profile
            List<Profile> allProfiles = profileRepository.findAll();
            Optional<Profile> residentProfile = allProfiles.stream()
                    .filter(p -> p.getUserId() == null)
                    .filter(p -> p.getFirstName().equals(resident.getFirstName()) 
                              && p.getLastName().equals(resident.getLastName()))
                    .findFirst();
            
            if (residentProfile.isPresent()) {
                Profile profile = residentProfile.get();
                profile.setUserId(user.getId());
                profileRepository.save(profile);
                LOGGER.info("✅ Profile ID: {} también vinculado", profile.getId());
            }
            
            LOGGER.info("✅ Resident (ID: {}) vinculado con User (ID: {}) [Primer resident disponible]", 
                    resident.getId(), user.getId());
            return;
        }
        
        // Si no existe resident pre-creado, significa que este residente se registró 
        // directamente en Auth0 (no debería pasar en producción)
        LOGGER.warn("⚠️ No hay Resident pre-creado para este usuario.");
        LOGGER.warn("⚠️ Los residentes deben ser creados por un Provider vía POST /residents/complete");
        LOGGER.warn("⚠️ Usuario ID: {} no tiene Resident asociado.", user.getId());
    }
}

