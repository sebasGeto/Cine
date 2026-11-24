package com.cineconecta.cuentas.dto;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El nombre se guarda sin espacios sobrantes y el correo, además, en minúscula. */
public record RegisterRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "no puede superar 150 caracteres")
        String fullName,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no es un correo válido")
        @Size(max = 254, message = "no puede superar 254 caracteres")
        String email,

        @NotBlank(message = "es obligatoria")
        @Size(min = 8, message = "debe tener al menos 8 caracteres")
        String password) {

    public RegisterRequest {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
