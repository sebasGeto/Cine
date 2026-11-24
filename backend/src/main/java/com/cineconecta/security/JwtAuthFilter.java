package com.cineconecta.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee "Authorization: Bearer <token>" y deja el AuthPrincipal en el SecurityContext.
 *
 * Un token inválido o vencido no corta la petición: sigue como anónima y las reglas de
 * SecurityConfig deciden. Así el login funciona aunque el navegador envíe un token viejo.
 *
 * No es @Component a propósito: lo crea SecurityConfig. Como bean, Spring Boot lo
 * registraría además como filtro del servidor, fuera de la cadena de seguridad.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            authenticate(header.substring(PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            AuthPrincipal principal = jwtService.parse(token);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }
    }
}
