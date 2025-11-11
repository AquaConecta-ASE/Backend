package com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.CreateProviderCommand;
import com.ironcoders.aquaconectabackend.profiles.domain.model.commands.UpdateProviderCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Provider extends AuditableAbstractAggregateRoot<Provider> {
    @Column(nullable = false)
    private String taxName;
    @Column(nullable = false)
    private String ruc;
    @Column(nullable = false)
    private Long userId;

    public Provider(String taxName, String ruc, Long userId) {
        this.taxName = taxName;
        this.ruc = ruc;
        this.userId = userId;
    }

    public Provider(CreateProviderCommand command){
        this.taxName= command.taxName();
        this.ruc= command.ruc();
        this.userId= command.userId();

    }

    public Provider() {

    }

    public void update(UpdateProviderCommand command) {
        this.taxName = command.taxName();
        this.ruc = command.ruc();
    }

    // Explicit getters (Lombok not working properly)
    public String getTaxName() {
        return taxName;
    }

    public String getRuc() {
        return ruc;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Long getId() {
        return super.getId();
    }
}
