package com.cineconecta.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    // Clave distinta en cada ejecución: no hay secretos escritos en las pruebas
    private final String secret = UUID.randomUUID() + "" + UUID.randomUUID();
    private final JwtService jwt = new JwtService(secret, 10);

    @Test
    void elTokenConservaIdYRol() {
        AuthPrincipal p = jwt.parse(jwt.generate("7", AuthPrincipal.CLIENT));

        assertThat(p).isEqualTo(new AuthPrincipal("7", "CLIENT"));
    }

    @Test
    void firmaConHs256AunqueLaClaveSeaLarga() {
        String header = jwt.generate("7", AuthPrincipal.ADMIN).split("\\.")[0];

        assertThat(new String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8)).contains("HS256");
    }

    @Test
    void rechazaTokenVencido() {
        String expired = new JwtService(secret, -1).generate("7", AuthPrincipal.CLIENT);

        assertThatThrownBy(() -> jwt.parse(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        String other = new JwtService(UUID.randomUUID() + "" + UUID.randomUUID(), 10).generate("7", AuthPrincipal.ADMIN);

        assertThatThrownBy(() -> jwt.parse(other)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaContenidoAlterado() {
        String[] parts = jwt.generate("7", AuthPrincipal.CLIENT).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("CLIENT", "ADMIN");
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        assertThatThrownBy(() -> jwt.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaRolDesconocido() {
        assertThatThrownBy(() -> jwt.parse(jwt.generate("7", "ROOT"))).isInstanceOf(JwtException.class);
    }

    @Test
    void exigeUnaClaveDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new JwtService("corta", 10)).isInstanceOf(IllegalStateException.class);
    }
}
