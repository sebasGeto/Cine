package com.cineconecta.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los tokens de sesión (JWT firmados con HS256).
 * Claims: sub = id del usuario, role = CLIENT o ADMIN, iat y exp.
 * La clave sale de JWT_SECRET (.env); nunca está en el código.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener 32 caracteres o más");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generate(String subject, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                // Se fija HS256: sin esto jjwt elige HS384 o HS512 según el largo de la clave
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma y vencimiento.
     *
     * @throws JwtException si el token es inválido, fue alterado o ya venció
     */
    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String role = claims.get(ROLE_CLAIM, String.class);
        boolean knownRole = AuthPrincipal.CLIENT.equals(role) || AuthPrincipal.ADMIN.equals(role);
        if (claims.getSubject() == null || claims.getExpiration() == null || !knownRole) {
            throw new JwtException("Token incompleto");
        }
        return new AuthPrincipal(claims.getSubject(), role);
    }
}
