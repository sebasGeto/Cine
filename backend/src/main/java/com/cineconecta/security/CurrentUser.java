package com.cineconecta.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.cineconecta.common.ApiException;

/**
 * Usuario de la petición actual. El accountId SIEMPRE se toma de aquí (del token firmado),
 * nunca de un parámetro o del cuerpo enviado por el cliente.
 */
@Component
public class CurrentUser {

    /** Id de la cuenta autenticada; 401 si la petición no es de un cliente. */
    public Integer accountId() {
        try {
            return Integer.valueOf(require(AuthPrincipal.CLIENT).id());
        } catch (NumberFormatException e) {
            throw unauthorized();
        }
    }

    /** Id del administrador autenticado; 401 si la petición no es de un administrador. */
    public String adminId() {
        return require(AuthPrincipal.ADMIN).id();
    }

    public boolean isAdmin() {
        AuthPrincipal principal = principal();
        return principal != null && AuthPrincipal.ADMIN.equals(principal.role());
    }

    private static AuthPrincipal require(String role) {
        AuthPrincipal principal = principal();
        if (principal == null || !role.equals(principal.role())) {
            throw unauthorized();
        }
        return principal;
    }

    private static AuthPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthPrincipal p ? p : null;
    }

    private static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Debe iniciar sesión");
    }
}
