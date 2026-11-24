package com.cineconecta.cuentas;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cineconecta.common.ApiException;
import com.cineconecta.cuentas.dto.LoginRequest;
import com.cineconecta.cuentas.dto.LoginResponse;
import com.cineconecta.cuentas.dto.RegisterRequest;
import com.cineconecta.cuentas.dto.RegisterResponse;
import com.cineconecta.security.AuthPrincipal;
import com.cineconecta.security.CredentialsVerifier;
import com.cineconecta.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** BCrypt solo admite 72 bytes (no caracteres: una tilde ocupa dos). */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AccountRepository accounts;
    private final CredentialsVerifier credentials;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * RF-01. No se consulta antes si el correo existe: dos registros simultáneos pasarían esa
     * consulta. El índice único ux_accounts_email_ci decide y su error se traduce a 409.
     */
    public RegisterResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "password: es demasiado larga");
        }
        Account account = new Account();
        account.setFullName(request.fullName());
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return new RegisterResponse(accounts.save(account).getAccountId());
    }

    /** RF-02. */
    public LoginResponse login(LoginRequest request) {
        Account account = accounts.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        credentials.verify(request.password(), account == null ? null : account.getPasswordHash());

        String token = jwtService.generate(account.getAccountId().toString(), AuthPrincipal.CLIENT);
        return new LoginResponse(token, AuthPrincipal.CLIENT, account.getAccountId(), account.getFullName());
    }
}
