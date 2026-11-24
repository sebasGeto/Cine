package com.cineconecta.cuentas.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "es obligatorio") String email,
        @NotBlank(message = "es obligatoria") String password) {
}
