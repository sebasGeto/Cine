package com.cineconecta.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cineconecta.common.ApiException;

/**
 * Verificación de contraseñas común a clientes y administradores.
 * Responde siempre lo mismo, sin decir si falló el usuario o la contraseña.
 */
@Component
public class CredentialsVerifier {

    private final PasswordEncoder passwordEncoder;

    /** Hash que no corresponde a nadie: iguala el tiempo de respuesta cuando el usuario no existe. */
    private final String dummyHash;

    public CredentialsVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** @param passwordHash hash guardado, o null si el usuario no existe */
    public void verify(String rawPassword, String passwordHash) {
        boolean matches = passwordEncoder.matches(rawPassword, passwordHash != null ? passwordHash : dummyHash);
        if (passwordHash == null || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Usuario o contraseña incorrectos");
        }
    }
}
