package com.cineconecta.catalogo;

import org.bson.types.ObjectId;

/**
 * Lo que se puede reproducir: una película, un documental o un episodio de una serie.
 * episodeId, seasonNumber y episodeNumber son null si no es una serie.
 */
public record PlayableItem(
        ObjectId contentId,
        String contentType,
        ObjectId episodeId,
        Integer seasonNumber,
        Integer episodeNumber,
        Integer durationSec) {
}
