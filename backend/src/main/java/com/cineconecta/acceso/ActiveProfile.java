package com.cineconecta.acceso;

/**
 * Perfil activo de la cuenta autenticada, ya validado contra PostgreSQL.
 * Es lo único que Mi lista y la reproducción necesitan saber del perfil.
 */
public record ActiveProfile(int accountId, int profileId, int maxAgeRating) {

    /** true si el perfil puede ver un contenido con esa clasificación. */
    public boolean allows(int ageRating) {
        return ageRating <= maxAgeRating;
    }
}
