package com.cineconecta.reproduccion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de POST /api/playback/sessions/{sessionId}/events (RF-28 a RF-30).
 *
 * type        pause | resume | seek | finish
 * positionSec posición del reproductor en el momento del evento (en seek, desde dónde se salta)
 * toSec       solo seek: a dónde se salta
 */
public record PlaybackEventRequest(
    @NotNull
    @Pattern(regexp = "pause|resume|seek|finish", message = "type debe ser pause, resume, seek o finish")
    String type,

    @NotNull
    @Min(0)
    Integer positionSec,

    @Min(0)
    Integer toSec
) {}
