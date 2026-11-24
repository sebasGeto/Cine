package com.cineconecta.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.cineconecta.common.ApiException;

class JdbcCatalogAccessTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcCatalogAccess access = new JdbcCatalogAccess(jdbc);

    private void profile(List<Integer> rows) {
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq(2), eq(1))).thenReturn(rows);
    }

    private void subscription(boolean current) {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(1))).thenReturn(current);
    }

    private void assertCode(String code) {
        assertThatThrownBy(() -> access.requireMaxAgeRating(1, 2))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(403);
                    assertThat(e.getCode()).isEqualTo(code);
                });
    }

    @Test
    void devuelveLaClasificacionDelPerfil() {
        profile(List.of(7));
        subscription(true);

        assertThat(access.requireMaxAgeRating(1, 2)).isEqualTo(7);
    }

    @Test
    void elPerfilSeVerificaAntesQueLaSuscripcion() {
        profile(List.of());
        subscription(false);

        assertCode("PROFILE_NOT_ALLOWED");
    }

    @Test
    void sinSuscripcionVigenteResponde403() {
        profile(List.of(7));
        subscription(false);

        assertCode("NO_ACTIVE_SUBSCRIPTION");
    }
}
