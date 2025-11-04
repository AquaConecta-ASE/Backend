package com.ironcoders.aquaconectabackend.requests.interfaces.rest.transform;

import com.ironcoders.aquaconectabackend.requests.domain.model.commands.CreateIssueReportCommand;
import com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources.CreateIssueReportResource;
public class CreateIssueReportCommandFromResourceAssembler {

    public static CreateIssueReportCommand toCommandFromResource(CreateIssueReportResource resource, Long userId) {
        return new CreateIssueReportCommand(
                resource.title(),
                resource.description(),
                resource.status(),
                userId
                

        );
    }

}