package com.cineconecta.reproduccion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.reproduccion.dto.ContinueWatchingItem;
import com.cineconecta.reproduccion.dto.HistoryPage;
import com.cineconecta.reproduccion.dto.PlaybackEventRequest;
import com.cineconecta.reproduccion.dto.SessionResponse;
import com.cineconecta.reproduccion.dto.StartPlaybackRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reproducción simulada y actividad del perfil (solo clientes). La cuenta sale del token y el
 * perfil del encabezado X-Profile-Id.
 *
 *   POST /api/playback/sessions                       iniciar (RF-27)            → 201
 *   POST /api/playback/sessions/{sessionId}/events    pausar, reanudar, avanzar
 *                                                     y finalizar (RF-28 a 30)   → 200
 *   GET  /api/playback/continue-watching              Continuar viendo (RF-31)   → 200
 *   GET  /api/playback/history?page=0&size=20         historial del perfil       → 200
 */
@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private static final String PROFILE_HEADER = "X-Profile-Id";

    private final PlaybackService playback;
    private final ViewingActivityService activity;

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@RequestHeader(PROFILE_HEADER) Integer profileId,
                                 @Valid @RequestBody StartPlaybackRequest request) {
        return playback.start(profileId, request);
    }

    @PostMapping("/sessions/{sessionId}/events")
    public SessionResponse event(@RequestHeader(PROFILE_HEADER) Integer profileId,
                                 @PathVariable String sessionId,
                                 @Valid @RequestBody PlaybackEventRequest request) {
        return playback.event(profileId, sessionId, request);
    }

    @GetMapping("/continue-watching")
    public List<ContinueWatchingItem> continueWatching(@RequestHeader(PROFILE_HEADER) Integer profileId) {
        return activity.continueWatching(profileId);
    }

    @GetMapping("/history")
    public HistoryPage history(@RequestHeader(PROFILE_HEADER) Integer profileId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return activity.history(profileId, page, size);
    }
}
