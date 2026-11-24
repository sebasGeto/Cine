package com.cineconecta.cuentas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.cuentas.dto.LoginRequest;
import com.cineconecta.cuentas.dto.LoginResponse;
import com.cineconecta.cuentas.dto.RegisterRequest;
import com.cineconecta.cuentas.dto.RegisterResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Sesión de los clientes. Rutas públicas. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** RF-01. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** RF-02. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
