package com.ironcoders.aquaconectabackend.iam.application.internal.commandservices;

import com.auth0.exception.Auth0Exception;
import com.ironcoders.aquaconectabackend.iam.application.internal.outboundservices.hashing.HashingService;
import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SignUpCommand;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.ironcoders.aquaconectabackend.shared.Infrastructure.auth0.Auth0ManagementService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component

public class SignUpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignUpHandler.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final Auth0ManagementService auth0ManagementService;

    public SignUpHandler(
            UserRepository userRepository, 
            RoleRepository roleRepository, 
            HashingService hashingService,
            Auth0ManagementService auth0ManagementService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.auth0ManagementService = auth0ManagementService;
    }


    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            return Optional.empty();

        List<Role> roles;

        if (command.roles() != null && !command.roles().isEmpty()) {
            // ✅ Asegurar que todos los roles existan (o crearlos si no)
            roles = command.roles().stream()
                    .map(role -> roleRepository.findByName(role.getName())
                            .orElseGet(() -> roleRepository.save(new Role(role.getName()))))
                    .toList();
        } else {
            // ⚠️ Si no vienen roles, usar ROLE_PROVIDER por defecto
            Role providerRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                    .orElseGet(() -> roleRepository.save(new Role(Roles.ROLE_PROVIDER)));
            roles = List.of(providerRole);
        }

        // Crear usuario en BD local
        var user = new User(command.username(), hashingService.encode(command.password()), roles);
        userRepository.save(user);
        
        // 🚀 Si se proporcionan datos de Auth0, crear también el usuario en Auth0
        if (command.email() != null && command.firstName() != null && command.lastName() != null) {
            try {
                LOGGER.info("🔐 Creando usuario en Auth0: {}", command.email());
                
                // Determinar el rol principal (el primero de la lista)
                String primaryRole = roles.isEmpty() ? "ROLE_USER" : roles.get(0).getName().name();
                
                // Crear usuario en Auth0 con rol en app_metadata
                String auth0UserId = createAuth0UserWithRole(
                    command.email(),
                    command.firstName(),
                    command.lastName(),
                    primaryRole,
                    command.providerId()
                );
                
                // Guardar el auth0UserId en el usuario local
                user.setAuth0Id(auth0UserId);
                userRepository.save(user);
                
                LOGGER.info("✅ Usuario creado en Auth0 con ID: {} y rol: {}", auth0UserId, primaryRole);
                
                // Enviar email de configuración de contraseña
                try {
                    String resetUrl = auth0ManagementService.sendPasswordSetupEmail(auth0UserId);
                    LOGGER.info("📧 Email de configuración enviado. URL de reset: {}", resetUrl);
                } catch (Auth0Exception emailError) {
                    LOGGER.warn("⚠️ Usuario creado pero falló el envío de email: {}", emailError.getMessage());
                }
                
            } catch (Auth0Exception e) {
                LOGGER.error("❌ Error creando usuario en Auth0: {}", e.getMessage(), e);
                // Continuar con el usuario local aunque falle Auth0
                LOGGER.warn("⚠️ Usuario creado solo en BD local, no en Auth0");
            }
        }
        
        return userRepository.findByUsername(command.username());
    }
    
    /**
     * Creates a user in Auth0 with the appropriate role in app_metadata.
     * Delegates to specific methods based on the role type.
     * 
     * @param email User's email
     * @param firstName User's first name
     * @param lastName User's last name  
     * @param roleName The role name (e.g., "ROLE_RESIDENT", "ROLE_PROVIDER")
     * @param providerId Optional provider ID (for residents)
     * @return Auth0 user ID
     * @throws Auth0Exception if creation fails
     */
    private String createAuth0UserWithRole(
            String email,
            String firstName,
            String lastName,
            String roleName,
            Long providerId) throws Auth0Exception {
        
        if ("ROLE_RESIDENT".equals(roleName)) {
            // Crear residente con providerId
            return auth0ManagementService.createResidentUser(
                email, 
                firstName, 
                lastName, 
                providerId
            );
        } else if ("ROLE_PROVIDER".equals(roleName)) {
            // Crear provider sin providerId
            return auth0ManagementService.createProviderUser(
                email, 
                firstName, 
                lastName
            );
        } else {
            // Para otros roles (ADMIN, etc.), usar creación de provider por defecto
            LOGGER.warn("⚠️ Rol {} no tiene método específico, usando creación genérica", roleName);
            return auth0ManagementService.createProviderUser(
                email, 
                firstName, 
                lastName
            );
        }
    }

}