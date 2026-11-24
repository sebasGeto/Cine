package com.cineconecta.perfiles;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cineconecta.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Contrato para las demás áreas: valida el perfil con el que navega el cliente (X-Profile-Id).
 * El accountId debe venir del token (CurrentUser.accountId()), nunca de la petición.
 */
@Service
@RequiredArgsConstructor
public class ProfileAccessService {

    private final ProfileRepository profiles;

    /**
     * Devuelve el perfil si es de la cuenta y está activo. En cualquier otro caso (no existe, es de
     * otra cuenta o está desactivado) responde 403 PROFILE_NOT_ALLOWED, sin decir cuál de ellos.
     */
    @Transactional(readOnly = true)
    public ActiveProfile verify(Integer accountId, Integer profileId) {
        if (accountId == null || profileId == null) {
            throw notAllowed();
        }
        return profiles.findByProfileIdAndAccountId(profileId, accountId)
                .filter(Profile::active)
                .map(ActiveProfile::from)
                .orElseThrow(ProfileAccessService::notAllowed);
    }

    private static ApiException notAllowed() {
        return new ApiException(HttpStatus.FORBIDDEN, "PROFILE_NOT_ALLOWED",
                "El perfil no pertenece a la cuenta o está desactivado");
    }
}
