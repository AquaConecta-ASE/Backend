package com.ironcoders.aquaconectabackend.profiles.application.internal.comandservices;

import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;
import com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.ironcoders.aquaconectabackend.iam.interfaces.acl.IamContextFacade;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl.DeviceContextFacade;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Provider;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProfileCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.dto.ResidentWithCredentials;
import com.ironcoders.aquaconectabackend.profiles.domain.model.valueobjects.PersonName;
import com.ironcoders.aquaconectabackend.profiles.domain.services.ResidentCommandService;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ProviderRepository;
import com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories.ResidentRepository;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ProfilesContextFacade.ProfilesContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription.SubscriptionCommandService;
import com.ironcoders.aquaconectabackend.subcriptions.interfaces.acl.SubscriptionContextFacade;

import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Service
public class ResidentCommandServiceImpl implements ResidentCommandService {

    private final ResidentRepository residentRepository;
    private final ProfileRepository profileRepository;
    private final IamContextFacade iamContextFacade;
    private final ProfilesContextFacade profilesContextFacade;
    private final ProviderRepository providerRepository;
    private final DeviceContextFacade deviceContextFacade;
    private final SubscriptionContextFacade subscriptionContextFacade;

    public ResidentCommandServiceImpl(
            ResidentRepository residentRepository,
            ProfileRepository profileRepository,
            IamContextFacade iamContextFacade,
            ProfilesContextFacade profilesContextFacade,
            ProviderRepository providerRepository,
            RoleRepository roleRepository,
            DeviceContextFacade deviceContextFacade,
            SubscriptionContextFacade subscriptionContextFacade // Cambiado el tipo aquí
    ) {
        this.residentRepository = residentRepository;
        this.profileRepository = profileRepository;
        this.iamContextFacade = iamContextFacade;
        this.profilesContextFacade = profilesContextFacade;
        this.providerRepository = providerRepository;
        this.deviceContextFacade = deviceContextFacade;
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    @Override
    public ResidentWithCredentials handle(CreateResidentCommand command) throws AccessDeniedException {
        Long providerId = command.providerId();

        List<Provider> providers = providerRepository.findByUserId(providerId);
        if (providers.isEmpty()) {
            throw new AccessDeniedException("Provider does not exist.");
        }
        Provider provider = providers.get(0);

        if (profileRepository.findByUserId(providerId).isEmpty()) {
            throw new IllegalArgumentException("No profile found for this provider.");
        }

        String username = command.firstName() + "." + command.lastName();
        String password = command.documentNumber();
        List<String> roles = List.of("ROLE_RESIDENT");

        // 🚀 Crear usuario con Auth0 integration
        Long newUserId = iamContextFacade.createUserWithAuth0(
            username, 
            password, 
            command.email(),
            command.firstName(),
            command.lastName(),
            roles,
            provider.getId()  // providerId for Auth0 app_metadata
        );
        
        if (newUserId == 0L) {
            throw new IllegalArgumentException("Could not create resident user.");
        }

        profilesContextFacade.createProfileForResident(
                newUserId,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.direction(),
                command.documentNumber(),
                command.documentType(),
                command.phone()
        );

        Resident resident = new Resident(command, newUserId, provider.getId());
        residentRepository.save(resident);

        // Crear el dispositivo y obtener el objeto Device
        var createDeviceCommand = new CreateDeviceCommand(
                "IOT",
                "ACTIVE",
                "TDS/HC-SR04",
                resident.getId()
        );
        Optional<Device> device = deviceContextFacade.createDevice(createDeviceCommand);

        if (device.isPresent()) {
            var createSubscriptionCommand = new CreateSubscriptionCommand(device.get().getId(), resident.getId(), resident.getProviderId(),command.waterTankSize());
            subscriptionContextFacade.createSubscription(createSubscriptionCommand); // Llama al service, no al facade
        }

        return new ResidentWithCredentials(resident, username, password);
    }

    @Override
    public Optional<Resident> handle(UpdateResidentCommand command) {
        Long userId = command.userId(); // Make sure your command has this method/field

        List<Resident> existingResident = residentRepository.findByUserId(userId);
        if (existingResident.isEmpty()) {
            throw new IllegalArgumentException("No resident found for this user");
        } 
        Resident resident = existingResident.get(0);
        resident.update(command);
        residentRepository.save(resident);

        return Optional.of(resident);
    }
}