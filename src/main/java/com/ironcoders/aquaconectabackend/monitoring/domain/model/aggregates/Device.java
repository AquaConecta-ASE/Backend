package com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateDeviceCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
public class Device extends AuditableAbstractAggregateRoot<Device> {

    @Column(nullable = false)
    private String type;
    @Column(nullable = false)

    private String status;
    @Column(nullable = false)

    private String description;
    @Column(nullable = false)
    private Long residentId;

    public Device() {}
    public Device(String type, String status, String description, Long residentId){
        this.type = type;
        this.status = status;
        this.description = description;
        this.residentId = residentId;

    }

    public Device(CreateDeviceCommand command) {
        this.type = command.type();
        this.status = command.status();
        this.description = command.description();
        this.residentId = command.residentId();

    }


}
