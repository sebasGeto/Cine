package com.cineconecta.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** username admite el usuario o el correo del administrador. */
public record AdminLoginRequest(
        @NotBlank(message = "es obligatorio") String username,
        @NotBlank(message = "es obligatoria") String password) {
}
