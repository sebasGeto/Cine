package com.cineconecta.catalogo;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.cineconecta.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Lee el perfil y la suscripción vigente de PostgreSQL (solo lectura), con los mismos códigos de
 * error que acuerda la sección 6 de la guía.
 *
 * PROVISIONAL: ProfileAccessService y SubscriptionService (B) aún no están en main. Cuando entren,
 * esta clase se reemplaza por una que llame a verify() y requireCurrent(); el resto del catálogo
 * solo conoce CatalogAccess y no cambia.
 */
@Component
@RequiredArgsConstructor
public class JdbcCatalogAccess implements CatalogAccess {

    private static final String PROFILE_SQL = """
            SELECT maxAgeRating FROM profiles
            WHERE profileId = ? AND accountId = ? AND isActive = 'Y'
            """;

    private static final String SUBSCRIPTION_SQL =
            "SELECT EXISTS (SELECT 1 FROM v_current_subscriptions WHERE accountId = ?)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int requireMaxAgeRating(Integer accountId, Integer profileId) {
        int maxAgeRating = jdbcTemplate.queryForList(PROFILE_SQL, Integer.class, profileId, accountId)
                .stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "PROFILE_NOT_ALLOWED",
                        "El perfil no pertenece a la cuenta o está desactivado"));

        if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject(SUBSCRIPTION_SQL, Boolean.class, accountId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NO_ACTIVE_SUBSCRIPTION",
                    "La cuenta no tiene una suscripción vigente");
        }
        return maxAgeRating;
    }
}
