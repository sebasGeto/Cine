package com.cineconecta.acceso;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.cineconecta.common.ApiException;
import com.cineconecta.security.CurrentUser;

/**
 * Valida el perfil leyendo la tabla profiles de PostgreSQL (fuente de verdad).
 * El accountId sale siempre del token (CurrentUser), nunca de la petición.
 *
 * PROVISIONAL: cuando exista ProfileAccessService (B), esta clase debe delegar en él
 * para que la regla de "perfil válido" viva en un solo lugar.
 */
@Component
public class JdbcProfileGate implements ProfileGate {

    private final JdbcTemplate jdbc;
    private final CurrentUser currentUser;

    public JdbcProfileGate(JdbcTemplate jdbc, CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @Override
    public ActiveProfile requireOwnActiveProfile(int profileId) {
        int accountId = currentUser.accountId();

        List<Row> rows = jdbc.query(
            "SELECT accountId, maxAgeRating, isActive FROM profiles WHERE profileId = ?",
            (rs, i) -> new Row(rs.getInt("accountId"), rs.getInt("maxAgeRating"),
                               "Y".equals(rs.getString("isActive"))),
            profileId);

        // Un perfil de otra cuenta se responde igual que uno inexistente, para no revelar que existe
        if (rows.isEmpty() || rows.get(0).accountId() != accountId) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "El perfil no existe");
        }
        Row row = rows.get(0);
        if (!row.active()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PROFILE_INACTIVE", "El perfil está desactivado");
        }
        return new ActiveProfile(accountId, profileId, row.maxAgeRating());
    }

    private record Row(int accountId, int maxAgeRating, boolean active) {}
}
