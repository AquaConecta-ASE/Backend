package com.ironcoders.aquaconectabackend.requests.domain.model.commands;

public record UpdateIssueReportCommand(Long id,
    String status,
    Long userId
) {
}




