package com.ironcoders.aquaconectabackend.subcriptions.application.internal.comandservices;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates.Device;
import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.monitoring.interfaces.rest.acl.DeviceContextFacade;
import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Resident;
import com.ironcoders.aquaconectabackend.profiles.interfaces.acl.ResidentContextFacade.ResidentContextFacade;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.aggregates.Subscription;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateAdditionalSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.CreateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.model.commands.UpdateSubscriptionCommand;
import com.ironcoders.aquaconectabackend.subcriptions.domain.services.subscription.SubscriptionCommandService;
import com.ironcoders.aquaconectabackend.subcriptions.infrastructure.persistence.jpa.repositories.subscription.SubscriptionRepository;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final SubscriptionRepository subscriptionRepository;
    private final ResidentContextFacade residentContextFacade;
    private final DeviceContextFacade deviceContextFacade;

    public SubscriptionCommandServiceImpl(
            SubscriptionRepository subscriptionRepository,
            ResidentContextFacade residentContextFacade,
            DeviceContextFacade deviceContextFacade
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.residentContextFacade = residentContextFacade;
        this.deviceContextFacade = deviceContextFacade;
    }

    @Override
    public Optional<Subscription> handle(CreateSubscriptionCommand command) {
        Subscription subscription = new Subscription(command);
        subscriptionRepository.save(subscription);
        return Optional.of(subscription);
    }

    @Override
    public Optional<Subscription> handle(UpdateSubscriptionCommand command) {
        Optional<Subscription> optional = subscriptionRepository.findById(command.id());

        if (optional.isEmpty()) {
            return Optional.empty();
        }

        Subscription subscription = optional.get();

        // Actualiza solo si vienen nuevos valores (opcional)
        if (command.status() != null) {
            subscription.setStatus(command.status());
        }
        if (command.endDate() != null) {
            subscription.setEndDate(command.endDate());
        }

        subscriptionRepository.save(subscription);
        return Optional.of(subscription);
    }

    @Override
    public Optional<Subscription> handle(CreateAdditionalSubscriptionCommand command) {

        // Busca la suscripción existente
        Optional<Resident> resident = residentContextFacade.findById(command.residentId());

        if (resident.isEmpty()) {
            return Optional.empty();
        }

        // Crear el dispositivo y obtener el objeto Device
        var createDeviceCommand = new CreateDeviceCommand(
            "IOT",
            "ACTIVE",
            "TDS/HC-SR04",
            resident.get().getId()
        );
        Optional<Device> device = deviceContextFacade.createDevice(createDeviceCommand);

        if (device.isEmpty()) {
            return Optional.empty();
        }
        Subscription additionalSubscription = new Subscription(
            command.residentId(),
            device.get().getId(),
            resident.get().getProviderId(),
            command.waterTankSize()
        );

        subscriptionRepository.save(additionalSubscription);
        return Optional.of(additionalSubscription);

    }
}