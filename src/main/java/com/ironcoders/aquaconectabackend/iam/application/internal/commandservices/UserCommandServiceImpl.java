package com.ironcoders.aquaconectabackend.iam.application.internal.commandservices;


import com.ironcoders.aquaconectabackend.iam.application.internal.outboundservices.hashing.HashingService;
import com.ironcoders.aquaconectabackend.iam.application.internal.outboundservices.tokens.TokenService;
import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SignInCommand;
import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SignUpCommand;
import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.domain.services.UserCommandService;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(UserRepository userRepository, HashingService hashingService, TokenService tokenService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
    }
    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            return Optional.empty();

        List<Role> roles;

        if (command.roles() != null && !command.roles().isEmpty()) {
            // 🔒 Verifica que todos los roles existan o créalos si no existen
            roles = command.roles().stream()
                    .map(role -> roleRepository.findByName(role.getName())
                            .orElseGet(() -> roleRepository.save(new Role(role.getName()))))
                    .toList();
        } else {
            // ⚠️ Si no se especifica nada, usar ROLE_PROVIDER por defecto
            Role providerRole = roleRepository.findByName(Roles.ROLE_PROVIDER)
                    .orElseGet(() -> roleRepository.save(new Role(Roles.ROLE_PROVIDER)));
            roles = List.of(providerRole);
        }

        var user = new User(command.username(), hashingService.encode(command.password()), roles);
        userRepository.save(user);

        return Optional.of(user);
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username());
        if (user.isEmpty()) throw new RuntimeException("User not found");
        if (!hashingService.matches(command.password(), user.get().getPassword()))
            throw new RuntimeException("Invalid password");
        var token = tokenService.generateToken(user.get().getUsername());
        return Optional.of(ImmutablePair.of(user.get(), token));
    }
}
