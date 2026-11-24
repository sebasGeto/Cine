package com.cineconecta.catalogo.dto;

import java.util.Comparator;
import java.util.List;

import com.cineconecta.catalogo.Season;

/** Temporada con sus episodios, siempre ordenados por número (en la base quedan como se agregaron). */
public record SeasonResponse(Integer number, String description, List<EpisodeResponse> episodes) {

    public static SeasonResponse from(Season s) {
        return new SeasonResponse(s.getNumber(), s.getDescription(), s.getEpisodes().stream()
                .map(EpisodeResponse::from)
                .sorted(Comparator.comparing(EpisodeResponse::number))
                .toList());
    }

    public static List<SeasonResponse> sorted(List<Season> seasons) {
        return seasons == null ? null : seasons.stream()
                .map(SeasonResponse::from)
                .sorted(Comparator.comparing(SeasonResponse::number))
                .toList();
    }
}
