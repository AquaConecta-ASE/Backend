package com.ironcoders.aquaconectabackend.iam.application.internal.commandservices;

import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AdminInitializer - Solo inicializa roles
 * 
 * IMPORTANTE: Con Auth0, NO se crean usuarios locales en el arranque.
 * Los usuarios se crean automáticamente cuando se autentican por primera vez
 * a través del Auth0UserSyncInterceptor.
 * 
 * Este componente solo garantiza que los roles necesarios existen en la BD.
 */
@Component
public class AdminInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializer.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminInitializer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeRoles() {
        LOGGER.info("🔧 Inicializando roles del sistema...");
        
        // Asegurar que todos los roles necesarios existen
        ensureRoleExists(Roles.ROLE_ADMIN);
        ensureRoleExists(Roles.ROLE_PROVIDER);
        ensureRoleExists(Roles.ROLE_RESIDENT);
        
        LOGGER.info("✅ Roles inicializados correctamente");
        
        // Información sobre usuarios
        long userCount = userRepository.count();
        LOGGER.info("ℹ️  Usuarios en BD: {}", userCount);
        LOGGER.info("ℹ️  Los usuarios se sincronizarán automáticamente desde Auth0 al autenticarse");
    }

    private void ensureRoleExists(Roles roleEnum) {
        roleRepository.findByName(roleEnum)
                .orElseGet(() -> {
                    Role newRole = roleRepository.save(new Role(roleEnum));
                    LOGGER.info("  ➕ Rol creado: {}", roleEnum.name());
                    return newRole;
                });
    }
}
