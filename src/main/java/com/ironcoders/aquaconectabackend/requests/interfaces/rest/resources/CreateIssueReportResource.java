package com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources;

public record CreateIssueReportResource(
        String title,
        String description,
        String status
) {}