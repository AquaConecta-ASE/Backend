package com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProfilesContextFacade;

import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ProfileCommandService;
import org.springframework.stereotype.Component;

@Component
public class ProfilesContextFacade {

    private final ProfileCommandService profileCommandService;

    public ProfilesContextFacade(ProfileCommandService profileCommandService) {
        this.profileCommandService = profileCommandService;
    }

    public void createProfileForResident(
            Long userId,
            String firstName,
            String lastName,
            String email,
            String direction,
            String documentNumber,
            String documentType,
            String phone
    ) {
        var command = new CreateProfileCommand(
                firstName,
                lastName,
                email,
                direction,
                documentNumber,
                documentType,
                phone,
                userId  
        );
        profileCommandService.handle(userId, command);
    }
}
