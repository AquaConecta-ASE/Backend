package com.ironcoders.aquaconectabackend.requests.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateWaterSupplyRequestCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WaterSupplyRequest extends AuditableAbstractAggregateRoot<WaterSupplyRequest> {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private String requestedLiters;

    @Column(nullable = false)
    private String emissionDate;

    @Column(nullable = false)
    private String status;

    @Column(nullable = true)
    private LocalDateTime deliveredAt;

    public WaterSupplyRequest(Long residentId, Long providerId, String requestedLiters, String status, LocalDateTime deliveredAt) {
        this.residentId = residentId;
        this.providerId = providerId;
        this.requestedLiters = requestedLiters;
        this.status = status;
        this.deliveredAt = null;
        this.emissionDate= LocalDateTime.now().toString();

    }

    public WaterSupplyRequest(CreateWaterSupplyRequestCommand command) {
        this.residentId = command.residentId();
        this.providerId = command.providerId();
        this.requestedLiters = command.requestedLiters();
        this.status = command.status();
        this.deliveredAt = command.deliveredAt();
        this.emissionDate= LocalDateTime.now().toString();
    }

    public WaterSupplyRequest update(String status, LocalDateTime deliveredAt) {
        this.status = status;
        this.deliveredAt = deliveredAt;
        return this;
    }
    
    public WaterSupplyRequest() {
    }

    // Explicit getters
    @Override
    public Long getId() {
        return super.getId();
    }

    public Long getResidentId() {
        return residentId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getRequestedLiters() {
        return requestedLiters;
    }

    public String getEmissionDate() {
        return emissionDate;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
}
