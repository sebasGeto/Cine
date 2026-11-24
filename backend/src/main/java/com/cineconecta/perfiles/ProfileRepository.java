package com.cineconecta.perfiles;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    List<Profile> findByAccountIdOrderByProfileId(Integer accountId);

    /** El perfil solo se encuentra si es de esa cuenta. */
    Optional<Profile> findByProfileIdAndAccountId(Integer profileId, Integer accountId);
}
