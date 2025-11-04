package com.ironcoders.aquaconectabackend.iam.application.internal.commandservices;

import com.ironcoders.aquaconectabackend.iam.application.internal.outboundservices.hashing.HashingService;
import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SignUpCommand;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component

public class SignUpHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;

    public SignUpHandler(UserRepository userRepository, RoleRepository roleRepository, HashingService hashingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
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

        var user = new User(command.username(), hashingService.encode(command.password()), roles);
        userRepository.save(user);
        return userRepository.findByUsername(command.username());
    }

}