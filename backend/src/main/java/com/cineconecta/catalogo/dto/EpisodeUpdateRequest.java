package com.cineconecta.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cambios a un episodio. El número va en la ruta y, como el episodeId, no cambia. */
public record EpisodeUpdateRequest(
        @NotBlank(message = "es obligatorio") String title,
        @NotBlank(message = "es obligatoria") String synopsis,
        @NotNull(message = "es obligatoria") @Min(value = 1, message = "debe ser 1 o más") Integer durationSec) {
}
