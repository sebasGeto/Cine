package com.cineconecta.catalogo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Temporada nueva. episodes es opcional: los episodios también se agregan uno por uno. */
public record SeasonRequest(
        @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser 1 o más") Integer number,
        @NotBlank(message = "es obligatoria") String description,
        @UniqueNumbers(message = "hay números de episodio repetidos")
        List<@NotNull(message = "no puede ser nulo") @Valid EpisodeRequest> episodes)
        implements Numbered {
}
