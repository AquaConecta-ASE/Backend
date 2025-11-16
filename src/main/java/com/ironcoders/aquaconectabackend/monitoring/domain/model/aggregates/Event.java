package com.ironcoders.aquaconectabackend.monitoring.domain.model.aggregates;

import com.ironcoders.aquaconectabackend.monitoring.domain.model.commads.CreateEventCommand;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    // Explicit getters
    @Override
    public Long getId() {
        return super.getId();
    }

    public String getEventType() {
        return eventType;
    }

    public String getQualityValue() {
        return qualityValue;
    }

    public String getLevelValue() {
        return levelValue;
    }

    public Long getSensorId() {
        return sensorId;
    }
}