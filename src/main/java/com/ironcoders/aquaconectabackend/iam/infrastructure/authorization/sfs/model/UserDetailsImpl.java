package com.ironcoders.aquaconectabackend.iam.infrastructure.authorization.sfs.model;

import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {
    private final Long id; // Add this line

    private final String username;
    @JsonIgnore
    private final String password;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    public static UserDetailsImpl build(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getStringName()))
                .collect(Collectors.toList());
        return new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword(), authorities); // Modify this line
    }

    public boolean isFarmer() {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARMER"));
    }

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isOwner() {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
    }

    public boolean isFarmWorker() {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARMWORKER"));
    }

    public Long getUserDetailsId(){
        return this.id;
    }


}