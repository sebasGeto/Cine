package com.cineconecta.reproduccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de POST /api/playback/sessions (RF-27).
 * episodeId es obligatorio en las series (las series se reproducen por episodio) y se ignora en
 * películas y documentales. Ambos son ObjectId en texto (24 caracteres hexadecimales).
 */
public record StartPlaybackRequest(
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{24}$", message = "contentId no es un identificador válido")
    String contentId,

    @Pattern(regexp = "^[0-9a-fA-F]{24}$", message = "episodeId no es un identificador válido")
    String episodeId
) {}
