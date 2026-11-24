package com.cineconecta.perfiles.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Crear o editar un perfil. El nombre se guarda sin espacios sobrantes. */
public record ProfileRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 60, message = "no puede superar 60 caracteres")
        String name,

        @NotNull(message = "es obligatoria")
        @Min(value = 0, message = "debe estar entre 0 y 18")
        @Max(value = 18, message = "debe estar entre 0 y 18")
        Integer maxAgeRating) {

    public ProfileRequest {
        name = name == null ? null : name.strip();
    }
}
