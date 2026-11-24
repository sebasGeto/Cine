package com.cineconecta.catalogo.dto;

import jakarta.validation.constraints.NotBlank;

/** Cambios a una temporada. El número va en la ruta y no cambia. */
public record SeasonUpdateRequest(
        @NotBlank(message = "es obligatoria") String description) {
}
