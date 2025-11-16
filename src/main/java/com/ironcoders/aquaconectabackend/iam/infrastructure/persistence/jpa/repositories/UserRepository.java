package com.ironcoders.aquaconectabackend.iam.infrastructure.persistence.jpa.repositories;


import com.ironcoders.aquaconectabackend.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuth0Id(String auth0Id);
    boolean existsByAuth0Id(String auth0Id);
    
    // Methods for username-based queries (used in legacy and Auth0 sync)
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
