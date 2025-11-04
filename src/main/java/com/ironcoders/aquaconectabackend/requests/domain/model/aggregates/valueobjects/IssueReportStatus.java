package com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.valueobjects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Arrays;
import java.util.Objects;


@Embeddable
public class IssueReportStatus {

    public enum Status {
        RECEIVED,
        IN_PROGRESS,
        CLOSED
    }

    @Enumerated(EnumType.STRING)
    private Status value;

    protected IssueReportStatus() {} // Constructor para JPA

    public IssueReportStatus(Status value) {
        this.value = value;
    }

    public IssueReportStatus(String value) {
        this.value = Status.valueOf(value.trim().toUpperCase().replace(" ", "_"));
    }

    public Status getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueReportStatus that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.name();
    }
}