package com.cineconecta.catalogo.dto;

import com.cineconecta.catalogo.Episode;

public record EpisodeResponse(
        String episodeId,
        Integer number,
        String title,
        String synopsis,
        Integer durationSec) {

    public static EpisodeResponse from(Episode e) {
        return new EpisodeResponse(e.getEpisodeId().toHexString(), e.getNumber(), e.getTitle(),
                e.getSynopsis(), e.getDurationSec());
    }
}
