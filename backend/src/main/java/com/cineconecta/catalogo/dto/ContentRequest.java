package com.cineconecta.catalogo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de un contenido para crearlo (RF-14) o actualizarlo (RF-15). Los campos comunes se
 * validan aquí; qué campos exige y admite cada tipo, en ContentFieldsByType.
 *
 * seasons solo se usa al crear: después cada temporada y episodio tiene su propia ruta, para
 * que los episodeId no cambien.
 */
@ContentFieldsByType
public record ContentRequest(
        @NotNull(message = "es obligatorio")
        @Pattern(regexp = "movie|series|documentary", message = "debe ser movie, series o documentary")
        String type,

        @NotBlank(message = "es obligatorio") String title,
        @NotBlank(message = "es obligatoria") String synopsis,

        @NotNull(message = "es obligatorio")
        @Min(value = 1888, message = "debe ser 1888 o posterior")
        @Max(value = 2100, message = "debe ser 2100 o anterior")
        Integer releaseYear,

        @NotEmpty(message = "debe tener al menos uno")
        List<@NotBlank(message = "no puede estar vacío") String> genres,

        @NotEmpty(message = "debe tener al menos uno")
        List<@NotBlank(message = "no puede estar vacío") String> countries,

        @NotEmpty(message = "debe tener al menos uno")
        List<@NotBlank(message = "no puede estar vacío") String> audioLanguages,

        @NotNull(message = "es obligatorio (puede ser una lista vacía)")
        List<@NotBlank(message = "no puede estar vacío") String> subtitleLanguages,

        @NotNull(message = "es obligatoria") Integer ageRating,

        @NotBlank(message = "es obligatoria")
        @Pattern(regexp = "\\s*|\\s*https?://\\S+\\s*", message = "debe empezar por http:// o https://")
        String coverUrl,

        // Película y documental
        @Min(value = 1, message = "debe ser 1 o más") Integer durationSec,
        List<@NotBlank(message = "no puede estar vacío") String> directors,

        // Película
        List<@NotNull(message = "no puede ser nulo") @Valid CastMemberDto> cast,

        // Documental
        List<@NotBlank(message = "no puede estar vacío") String> topics,
        List<@NotNull(message = "no puede ser nulo") @Valid IntervieweeDto> interviewees,
        List<@NotNull(message = "no puede ser nulo") @Valid SourceDto> sources,

        // Serie
        @UniqueNumbers(message = "hay números de temporada repetidos")
        List<@NotNull(message = "no puede ser nulo") @Valid SeasonRequest> seasons) {
}
