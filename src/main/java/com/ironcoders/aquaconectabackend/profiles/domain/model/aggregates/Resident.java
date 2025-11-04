package com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateResidentCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateResidentCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
public class Resident extends AuditableAbstractAggregateRoot<Resident> {
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long providerId;

    public Resident(String firstName, String ruc, Long userId, Long providerId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
        this.providerId = providerId;
    }

    public Resident(CreateResidentCommand command, Long userId, Long providerId){
        this.firstName= command.firstName();
        this.lastName= command.lastName();
        this.userId = userId;
        this.providerId= providerId;


    }

    public Resident() {

    }

    public void update(UpdateResidentCommand command) {
        this.firstName = command.firstName();
        this.lastName = command.lastName();
    }




}
