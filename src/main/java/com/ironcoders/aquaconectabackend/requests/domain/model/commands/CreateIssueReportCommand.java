package com.ironcoders.aquaconectabackend.requests.domain.model.commands;

public record CreateIssueReportCommand(
    String title,
    String description,
    String status,
    Long userId
) {

}
