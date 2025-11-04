package com.ironcoders.aquaconectabackend.profiles.application.internal.comandservices;

import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.valueobjects.PersonName;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileCommandService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {
    private final ProfileRepository profileRepository;

    public ProfileCommandServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

   @Override
    public Optional<Profile> handle(CreateProfileCommand command) {
        // Check if a profile already exists for the user
        List<Profile> existingProfile = profileRepository.findByUserId(command.userId());
        if (!existingProfile.isEmpty()) {
            throw new IllegalArgumentException("A profile already exists for this user");
        }

        List<Profile> profilesByEmail = profileRepository.findByEmail(command.email());
        List<Profile> profilesByDocumentNumber = profileRepository.findByDocumentNumber(command.documentNumber());

        if (!profilesByEmail.isEmpty()) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (!profilesByDocumentNumber.isEmpty()) {
            throw new IllegalArgumentException("Document number already exists");
        }

        var profile = new Profile(command);

        profileRepository.save(profile);

        return Optional.of(profile);
    }

    public Optional<Profile> handle(UpdateProfileCommand command) {
        // Buscar el perfil por el userId que viene en el comando
        List<Profile> existingProfile = profileRepository.findByUserId(command.userId());
        if (existingProfile.isEmpty()) {
            throw new IllegalArgumentException("No profile found for this user");
        }

        Profile profile = existingProfile.get(0);
        profile.update(command);
        profileRepository.save(profile);

        return Optional.of(profile);
    }

    @Override
    public void handle(Long userId, CreateProfileCommand command) {
        // Check if a profile already exists for the user
        List<Profile> existingProfile = profileRepository.findByUserId(userId);

        if( !existingProfile.isEmpty() ) {
            throw new IllegalArgumentException("A profile already exists for this user");
        }

        PersonName name = new PersonName(command.firstName(), command.lastName());

        Profile profile = new Profile(
                name,
                command.email(),
                command.direction(),
                command.documentNumber(),
                command.documentType(),
                userId,
                command.phone()
        );
        profileRepository.save(profile);
    }


}