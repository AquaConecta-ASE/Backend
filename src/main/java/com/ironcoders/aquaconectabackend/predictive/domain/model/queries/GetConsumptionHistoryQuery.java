package com.ironcoders.aquaconectabackend.predictive.domain.model.queries;

import java.time.LocalDate;

public record GetConsumptionHistoryQuery(
    Long residentId,
    LocalDate startDate,
    LocalDate endDate
) {
}
