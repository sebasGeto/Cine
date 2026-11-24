package com.cineconecta.security;

/**
 * Usuario autenticado, tal como viene en el token.
 * id = accountId de PostgreSQL (CLIENT) o _id del administrador en MongoDB (ADMIN).
 */
public record AuthPrincipal(String id, String role) {

    public static final String CLIENT = "CLIENT";
    public static final String ADMIN = "ADMIN";
}
