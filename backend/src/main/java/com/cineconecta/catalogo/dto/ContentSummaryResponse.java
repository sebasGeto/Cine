package com.cineconecta.catalogo.dto;

import java.util.List;

import com.cineconecta.catalogo.Content;

/** Resultado de la búsqueda del cliente (RF-21). */
public record ContentSummaryResponse(
        String id,
        String type,
        String title,
        Integer releaseYear,
        Integer ageRating,
        String coverUrl,
        List<String> genres) {

    public static ContentSummaryResponse from(Content c) {
        return new ContentSummaryResponse(c.getId().toHexString(), c.getType(), c.getTitle(),
                c.getReleaseYear(), c.getAgeRating(), c.getCoverUrl(), c.getGenres());
    }
}
