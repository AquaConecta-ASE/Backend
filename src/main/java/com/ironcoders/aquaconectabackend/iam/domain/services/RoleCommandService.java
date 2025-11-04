package com.ironcoders.aquaconectabackend.iam.domain.services;


import com.ironcoders.aquaconectabackend.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
