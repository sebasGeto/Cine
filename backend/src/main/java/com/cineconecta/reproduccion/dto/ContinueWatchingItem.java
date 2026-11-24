package com.cineconecta.reproduccion.dto;

import java.time.Instant;

/**
 * Un elemento de "Continuar viendo" (RF-31). En las series, episodeId/seasonNumber/episodeNumber
 * indican el episodio en curso (puede ser el siguiente al último que se terminó, desde 0).
 * Para retomar: POST /api/playback/sessions con contentId y episodeId.
 */
public record ContinueWatchingItem(
    String contentId,
    String title,
    String contentType,
    String coverUrl,
    String episodeId,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer positionSec,
    Integer durationSec,
    int progressPercent,
    Instant lastActivityAt
) {}
