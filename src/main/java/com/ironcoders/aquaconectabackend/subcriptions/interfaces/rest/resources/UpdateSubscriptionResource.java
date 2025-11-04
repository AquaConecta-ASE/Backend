package com.ironcoders.aquaconectabackend.subcriptions.interfaces.rest.resources;

import java.time.LocalDate;

public record UpdateSubscriptionResource(
        String status,
        LocalDate endDate
) {}
