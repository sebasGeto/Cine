package com.cineconecta.cuentas;

import org.springframework.stereotype.Service;

import com.cineconecta.cuentas.dto.LoginRequest;
import com.cineconecta.cuentas.dto.LoginResponse;
import com.cineconecta.security.AuthPrincipal;
import com.cineconecta.security.CredentialsVerifier;
import com.cineconecta.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accounts;
    private final CredentialsVerifier credentials;
    private final JwtService jwtService;

    /** RF-02. */
    public LoginResponse login(LoginRequest request) {
        Account account = accounts.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        credentials.verify(request.password(), account == null ? null : account.getPasswordHash());

        String token = jwtService.generate(account.getAccountId().toString(), AuthPrincipal.CLIENT);
        return new LoginResponse(token, AuthPrincipal.CLIENT, account.getAccountId(), account.getFullName());
    }
}
