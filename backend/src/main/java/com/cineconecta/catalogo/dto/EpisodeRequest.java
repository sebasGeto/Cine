package com.cineconecta.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Episodio nuevo. El episodeId no se recibe: lo genera el servidor. */
public record EpisodeRequest(
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser 1 o más") Integer number,
        @NotBlank(message = "es obligatorio") String title,
        @NotBlank(message = "es obligatoria") String synopsis,
        @NotNull(message = "es obligatoria") @Min(value = 1, message = "debe ser 1 o más") Integer durationSec)
        implements Numbered {
}
