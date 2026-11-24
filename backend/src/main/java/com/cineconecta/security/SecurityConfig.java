package com.cineconecta.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;

/**
 * Seguridad de la API: sin estado, autenticada con JWT y con permisos por rol.
 *
 *   Públicas            /api/health, /api/auth/**, GET /api/plans
 *   Solo ADMIN          /api/admin/**
 *   Solo CLIENT         todo lo demás
 *
 * El administrador no es un cliente: no puede usar las rutas de cliente, ni al revés.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   SecurityErrorHandler errorHandler) throws Exception {
        http
            // Sin cookies de sesión no hay riesgo de CSRF: el token viaja en un encabezado
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler))
            .authorizeHttpRequests(auth -> auth
                // Reenvío interno a /error: si no se permite, un 500 terminaría respondiendo 401
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/health", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/plans").permitAll()
                .requestMatchers("/api/admin/**").hasRole(AuthPrincipal.ADMIN)
                .anyRequest().hasRole(AuthPrincipal.CLIENT))
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Hash de contraseñas de clientes y administradores. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
