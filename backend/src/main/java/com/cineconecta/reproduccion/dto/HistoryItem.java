package com.cineconecta.reproduccion.dto;

import java.time.Instant;

/** Una sesión del historial de visualización del perfil. title es null si el contenido ya no existe. */
public record HistoryItem(
    String sessionId,
    String contentId,
    String title,
    String contentType,
    String episodeId,
    Integer seasonNumber,
    Integer episodeNumber,
    String status,
    Integer watchedSec,
    Integer lastPositionSec,
    Integer durationSec,
    boolean completed,
    Instant startedAt,
    Instant endedAt
) {}
