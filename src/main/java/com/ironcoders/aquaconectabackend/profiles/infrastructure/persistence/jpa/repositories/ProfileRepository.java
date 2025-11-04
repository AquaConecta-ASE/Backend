package com.ironcoders.aquaconectabackend.profiles.infrastructure.persistence.jpa.repositories;


import com.ironcoders.aquaconectabackend.profiles.domain.model.aggregates.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findByEmail(String email);
    List<Profile> findByDocumentNumber(String documentNumber);
    List<Profile> findByUserId(long userId);

}