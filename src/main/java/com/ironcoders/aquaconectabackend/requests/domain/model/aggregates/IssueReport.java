package com.ironcoders.aquaconectabackend.requests.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateIssueReportCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
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


    //    public EventAggregate(CreateEventCommand command){
    //    this.eventType= command.eventType();
    //    this.qualityValue = command.qualityValue();
    //    this.levelValue = command.levelValue();
    //    this.sensorId = command.sensorId();
   // }
    //
    
}
