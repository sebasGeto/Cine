package com.cineconecta.reproduccion.dto;

import java.time.Instant;

import com.cineconecta.reproduccion.ViewingSession;

/** Estado de una sesión de reproducción, tal como lo necesita el reproductor del frontend. */
public record SessionResponse(
    String sessionId,
    String contentId,
    String contentType,
    String episodeId,
    Integer seasonNumber,
    Integer episodeNumber,
    Integer durationSec,
    String status,
    boolean playing,
    Integer positionSec,
    Integer watchedSec,
    boolean completed,
    Instant startedAt,
    Instant endedAt
) {

    public static SessionResponse from(ViewingSession s) {
        return new SessionResponse(
            s.getId().toHexString(),
            s.getContentId().toHexString(),
            s.getContentType(),
            s.getEpisodeId() == null ? null : s.getEpisodeId().toHexString(),
            s.getSeasonNumber(),
            s.getEpisodeNumber(),
            s.getDurationSec(),
            s.getStatus(),
            s.getPlayState() != null && Boolean.TRUE.equals(s.getPlayState().getPlaying()),
            s.getLastPositionSec(),
            s.getWatchedSec(),
            Boolean.TRUE.equals(s.getCompleted()),
            s.getStartedAt(),
            s.getEndedAt());
    }
}
