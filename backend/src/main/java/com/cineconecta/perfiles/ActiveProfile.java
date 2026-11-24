package com.cineconecta.perfiles;

/**
 * Perfil activo de la cuenta autenticada, ya verificado contra PostgreSQL.
 * Es lo que catálogo, Mi lista y reproducción necesitan saber del perfil (X-Profile-Id).
 */
public record ActiveProfile(Integer profileId, Integer accountId, String name, Integer maxAgeRating) {

    static ActiveProfile from(Profile profile) {
        return new ActiveProfile(profile.getProfileId(), profile.getAccountId(), profile.getName(),
                profile.getMaxAgeRating());
    }
}
