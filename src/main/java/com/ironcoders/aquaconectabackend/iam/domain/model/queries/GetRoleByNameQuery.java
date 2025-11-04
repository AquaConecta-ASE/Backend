package com.ironcoders.aquaconectabackend.iam.domain.model.queries;


import com.ironcoders.aquaconectabackend.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles roleName) {
}
