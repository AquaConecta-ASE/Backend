package com.ironcoders.aquaconectabackend.requests.domain.services;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

import com.ironcoders.aquaconectabackend.requests.domain.model.aggregates.IssueReport;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateIssueReportCommand;
import com.ironcoders.aquaconectabackend.requests.domain.model.commands.UpdateIssueReportCommand;

public interface IssueReportCommandService {
  Optional<IssueReport> handle(CreateIssueReportCommand command) throws AccessDeniedException;
  Optional<IssueReport> handle(UpdateIssueReportCommand command) throws AccessDeniedException;
}
