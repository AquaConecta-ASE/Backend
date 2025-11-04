package com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
public class Event extends AuditableAbstractAggregateRoot<Event> {

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String qualityValue;

    @Column(nullable = false)
    private String levelValue;

    @Column(nullable = false)
    private Long sensorId;

    public Event() {}

    public Event(String eventType, String qualityValue, String levelValue, Long sensorId) {
        this.eventType = eventType;
        this.qualityValue = qualityValue;
        this.levelValue = levelValue;
        this.sensorId = sensorId;
    }

    public Event(CreateEventCommand command){
        this.eventType= command.eventType();
        this.qualityValue = command.qualityValue();
        this.levelValue = command.levelValue();
        this.sensorId = command.sensorId();
    }


}