package com.cineconecta.catalogo;

import org.springframework.stereotype.Component;

import com.cineconecta.perfiles.ProfileAccessService;
import com.cineconecta.suscripciones.SubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * Requisito de las rutas de cliente del catálogo: perfil activo de la cuenta y suscripción
 * vigente. Usa la misma verificación que Mi lista y la reproducción; aquí no se repite ninguna regla.
 */
@Component
@RequiredArgsConstructor
public class CatalogAccess {

    private final ProfileAccessService profiles;
    private final SubscriptionService subscriptions;

    /**
     * @param accountId cuenta del token (CurrentUser), nunca de la petición
     * @param profileId perfil del encabezado X-Profile-Id
     * @return clasificación máxima permitida para el perfil
     * @throws com.cineconecta.common.ApiException 403 PROFILE_NOT_ALLOWED si el perfil no es de la
     *         cuenta o está inactivo; 403 NO_ACTIVE_SUBSCRIPTION si no hay suscripción vigente
     */
    public int requireMaxAgeRating(Integer accountId, Integer profileId) {
        int maxAgeRating = profiles.verify(accountId, profileId).maxAgeRating();
        subscriptions.requireCurrent(accountId);
        return maxAgeRating;
    }
}
