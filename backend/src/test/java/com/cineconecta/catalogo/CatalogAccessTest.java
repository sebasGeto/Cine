package com.cineconecta.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.cineconecta.common.ApiException;
import com.cineconecta.perfiles.ActiveProfile;
import com.cineconecta.perfiles.ProfileAccessService;
import com.cineconecta.suscripciones.SubscriptionService;

class CatalogAccessTest {

    private final ProfileAccessService profiles = mock(ProfileAccessService.class);
    private final SubscriptionService subscriptions = mock(SubscriptionService.class);
    private final CatalogAccess access = new CatalogAccess(profiles, subscriptions);

    private static ApiException forbidden(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "x");
    }

    @Test
    void devuelveLaClasificacionDelPerfilSiHaySuscripcionVigente() {
        when(profiles.verify(1, 2)).thenReturn(new ActiveProfile(2, 1, "Tomás", 7));

        assertThat(access.requireMaxAgeRating(1, 2)).isEqualTo(7);
        verify(subscriptions).requireCurrent(1);
    }

    @Test
    void elPerfilSeVerificaAntesQueLaSuscripcion() {
        ApiException error = forbidden("PROFILE_NOT_ALLOWED");
        when(profiles.verify(1, 2)).thenThrow(error);

        assertThatThrownBy(() -> access.requireMaxAgeRating(1, 2)).isSameAs(error);
        verifyNoInteractions(subscriptions);
    }

    @Test
    void sinSuscripcionVigenteNoSePermite() {
        ApiException error = forbidden("NO_ACTIVE_SUBSCRIPTION");
        when(profiles.verify(1, 2)).thenReturn(new ActiveProfile(2, 1, "Tomás", 7));
        when(subscriptions.requireCurrent(1)).thenThrow(error);

        assertThatThrownBy(() -> access.requireMaxAgeRating(1, 2)).isSameAs(error);
    }
}
