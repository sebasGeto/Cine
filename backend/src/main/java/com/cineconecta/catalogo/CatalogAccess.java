package com.cineconecta.catalogo;

/**
 * Requisito de las rutas de cliente del catálogo: perfil activo de la cuenta y suscripción
 * vigente. Único punto por el que el catálogo depende de perfiles y suscripciones (B).
 */
public interface CatalogAccess {

    /**
     * @param accountId cuenta del token (CurrentUser), nunca de la petición
     * @param profileId perfil del encabezado X-Profile-Id
     * @return clasificación máxima permitida para el perfil
     * @throws com.cineconecta.common.ApiException 403 PROFILE_NOT_ALLOWED si el perfil no es de la
     *         cuenta o está inactivo; 403 NO_ACTIVE_SUBSCRIPTION si no hay suscripción vigente
     */
    int requireMaxAgeRating(Integer accountId, Integer profileId);
}
