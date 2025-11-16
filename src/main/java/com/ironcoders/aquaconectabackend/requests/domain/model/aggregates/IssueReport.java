package com.ironcoders.aquaconectabackend.requests.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateIssueReportCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
public class IssueReport extends AuditableAbstractAggregateRoot<IssueReport> {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String emissionDate;


    @Column(nullable = false)
    private String status;

    public IssueReport() {}

    public IssueReport(Long residentId, Long providerId, String title, String description, String status) {
        this.residentId = residentId;
        this.providerId = providerId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.emissionDate= LocalDateTime.now().toString();
    }

    public IssueReport(CreateIssueReportCommand command) {
        this.title = command.title();
        this.description = command.description();
        this.status = command.status();
    }

    public IssueReport update(String status) {
        this.status = status;
        return this;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getEmissionDate() {
        return emissionDate;
    }

    public String getStatus() {
        return status;
    }
}
