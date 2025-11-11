package com.ironcoders.aquaconectabackend.iam.domain.model.aggregates;


import com.ironcoders.aquaconectabackend.iam.domain.model.entities.Role;
import com.ironcoders.aquaconectabackend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {
    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    // Identificador de Auth0
    @Column(unique = true, nullable = true)
    private String auth0Id;  // Este es el 'sub' del token JWT de Auth0

    @Size(max = 120)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    // Constructor vacío
    public User() {
        this.roles = new HashSet<>();
    }

    // Constructor principal para Auth0 (con auth0Id y roles)
    public User(String username, String auth0Id, List<Role> roles) {
        this.username = username;
        this.auth0Id = auth0Id;
        this.roles = new HashSet<>();
        addRoles(roles);
    }

    // Constructor solo con username y password (para compatibilidad temporal)
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>();
    }

    public User addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public User addRoles(List<Role> roles) {
        var validatedRoles = Role.validateRoleSet(roles);
        this.roles.addAll(validatedRoles);
        return this;
    }

    public @NotBlank @Size(max = 50) String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getAuth0Id() {
        return auth0Id;
    }

    // Setter para auth0Id
    public void setAuth0Id(String auth0Id) {
        this.auth0Id = auth0Id;
    }

    @Override
    public Long getId() {
        return super.getId();
    }
}
