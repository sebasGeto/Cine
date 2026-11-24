package com.cineconecta.admin;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.cineconecta.admin.dto.AdminLoginRequest;
import com.cineconecta.admin.dto.AdminLoginResponse;
import com.cineconecta.security.AuthPrincipal;
import com.cineconecta.security.CredentialsVerifier;
import com.cineconecta.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository admins;
    private final CredentialsVerifier credentials;
    private final JwtService jwtService;

    /** RF-33. Un administrador inactivo recibe la misma respuesta que uno inexistente. */
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser admin = admins.findActiveByUsernameOrEmail(request.username().trim()).orElse(null);
        credentials.verify(request.password(), admin == null ? null : admin.getPasswordHash());

        admins.updateLastLogin(admin.getId(), Instant.now());
        String token = jwtService.generate(admin.getId().toHexString(), AuthPrincipal.ADMIN);
        return new AdminLoginResponse(token, AuthPrincipal.ADMIN, admin.getFullName());
    }
}
