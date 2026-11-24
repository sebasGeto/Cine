package com.cineconecta.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.admin.dto.AdminLoginRequest;
import com.cineconecta.admin.dto.AdminLoginResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Sesión de los administradores. Ruta pública. */
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /** RF-33. */
    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }
}
