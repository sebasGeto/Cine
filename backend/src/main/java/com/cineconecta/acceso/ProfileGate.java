package com.cineconecta.acceso;

/**
 * Punto único por el que Mi lista y la reproducción validan el perfil.
 *
 * Cuando B suba ProfileAccessService, solo se cambia JdbcProfileGate para que
 * delegue en él; el resto del código de Mi lista y reproducción no cambia.
 */
public interface ProfileGate {

    /**
     * Devuelve el perfil si pertenece a la cuenta autenticada y está activo.
     * 404 PROFILE_NOT_FOUND si no existe o es de otra cuenta; 403 PROFILE_INACTIVE si está desactivado.
     */
    ActiveProfile requireOwnActiveProfile(int profileId);
}
