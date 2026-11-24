package com.cineconecta.reproduccion;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.cineconecta.catalogo.CatalogService;
import com.cineconecta.catalogo.PlayableItem;
import com.cineconecta.perfiles.ActiveProfile;
import com.cineconecta.perfiles.ProfileAccessService;
import com.cineconecta.reproduccion.ViewingSession.PlayState;
import com.cineconecta.reproduccion.ViewingSession.SessionEvent;
import com.cineconecta.reproduccion.dto.PlaybackEventRequest;
import com.cineconecta.reproduccion.dto.SessionResponse;
import com.cineconecta.reproduccion.dto.StartPlaybackRequest;
import com.cineconecta.security.CurrentUser;
import com.cineconecta.suscripciones.CurrentSubscription;
import com.cineconecta.suscripciones.SubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * Reproducción simulada (RF-27 a RF-30).
 *
 * Iniciar integra las dos bases: el perfil y la suscripción vigente salen de PostgreSQL (B) y el
 * contenido reproducible de MongoDB (C, requirePlayable). La sesión se escribe solo en MongoDB.
 *
 * Tiempo visto: el reproductor informa su posición en cada evento. Mientras suena, el tramo
 * [segmentStartSec, posición] cuenta como visto; un salto (seek) cierra el tramo y abre otro en el
 * destino, así que lo saltado no cuenta (RF-29).
 */
@Service
@RequiredArgsConstructor
public class PlaybackService {

    /** Una reproducción se considera completada al llegar al 90 % de la duración. */
    static final double COMPLETION_RATIO = 0.9;

    private final ViewingSessionRepository sessions;
    private final MongoTemplate mongo;
    private final ProfileAccessService profileAccess;
    private final SubscriptionService subscriptions;
    private final CatalogService catalog;
    private final ProgressService progress;
    private final CurrentUser currentUser;

    /**
     * RF-27. Verifica, en este orden: perfil activo de la cuenta (403 PROFILE_NOT_ALLOWED),
     * suscripción vigente (403 NO_ACTIVE_SUBSCRIPTION) y contenido publicado y permitido para el
     * perfil (404 CONTENT_NOT_AVAILABLE, 403 AGE_RATING_NOT_ALLOWED, 400 EPISODE_REQUIRED,
     * 404 EPISODE_NOT_FOUND). Retoma desde la posición guardada si no estaba completado.
     */
    public SessionResponse start(Integer profileId, StartPlaybackRequest request) {
        Integer accountId = currentUser.accountId();
        ActiveProfile profile = profileAccess.verify(accountId, profileId);
        CurrentSubscription subscription = subscriptions.requireCurrent(accountId);
        PlayableItem item = catalog.requirePlayable(
            PlaybackErrors.parseId(request.contentId(), "contentId"),
            PlaybackErrors.parseOptionalId(request.episodeId(), "episodeId"),
            profile.maxAgeRating());

        Instant now = Instant.now();
        // Un perfil reproduce una cosa a la vez: lo que quedó abierto se cierra antes de empezar
        closeOpenSessions(profile.profileId());

        int startAt = progress.resumePosition(profile.profileId(), item);

        ViewingSession session = new ViewingSession();
        session.setAccountId(accountId);
        session.setProfileId(profile.profileId());
        // Copias del momento de inicio: el informe de consumo usa el plan vigente al iniciar
        session.setSubscriptionId(subscription.subscriptionId());
        session.setPlanId(subscription.planId());
        session.setContentId(item.contentId());
        session.setContentType(item.contentType());
        session.setEpisodeId(item.episodeId());
        session.setSeasonNumber(item.seasonNumber());
        session.setEpisodeNumber(item.episodeNumber());
        session.setDurationSec(item.durationSec());
        session.setStartedAt(now);
        session.setStatus(ViewingSession.OPEN);
        session.setWatchedSec(0);
        session.setLastPositionSec(startAt);
        session.setCompleted(false);
        session.setLastActionAt(now);
        session.setPlayState(new PlayState(true, startAt));
        session.getEvents().add(SessionEvent.at("start", now, startAt));

        session = sessions.insert(session);
        progress.record(session, now);
        return SessionResponse.from(session);
    }

    /** RF-28 a RF-30: pausar, reanudar, avanzar y finalizar. */
    public SessionResponse event(Integer profileId, String sessionIdText, PlaybackEventRequest request) {
        ActiveProfile profile = profileAccess.verify(currentUser.accountId(), profileId);
        ViewingSession session = sessions
            .findByIdAndProfileId(PlaybackErrors.parseId(sessionIdText, "sessionId"), profile.profileId())
            .orElseThrow(PlaybackErrors::sessionNotFound);
        if (!session.isOpen()) {
            throw PlaybackErrors.sessionClosed();
        }

        Instant now = Instant.now();
        Instant previousActionAt = session.getLastActionAt();
        int duration = session.getDurationSec();
        int position = clamp(request.positionSec(), duration);
        PlayState state = session.getPlayState() != null ? session.getPlayState() : new PlayState(false, position);
        session.setPlayState(state);
        boolean playing = Boolean.TRUE.equals(state.getPlaying());

        switch (request.type()) {
            case "pause" -> {
                if (!playing) {
                    throw PlaybackErrors.invalidEvent("La reproducción ya está en pausa");
                }
                addWatched(session, position);
                state.setPlaying(false);
                session.setLastPositionSec(position);
                session.getEvents().add(SessionEvent.at("pause", now, position));
            }
            case "resume" -> {
                if (playing) {
                    throw PlaybackErrors.invalidEvent("La reproducción ya está en curso");
                }
                state.setPlaying(true);
                state.setSegmentStartSec(position);
                session.setLastPositionSec(position);
                session.getEvents().add(SessionEvent.at("resume", now, position));
            }
            case "seek" -> {
                if (request.toSec() == null) {
                    throw PlaybackErrors.invalidEvent("Para avanzar se debe indicar toSec");
                }
                int target = clamp(request.toSec(), duration);
                if (playing) {
                    // Lo visto hasta el salto cuenta; lo saltado no
                    addWatched(session, position);
                    state.setSegmentStartSec(target);
                }
                session.setLastPositionSec(target);
                session.getEvents().add(SessionEvent.seek(now, position, target));
            }
            case "finish" -> {
                if (playing) {
                    addWatched(session, position);
                }
                state.setPlaying(false);
                session.setLastPositionSec(position);
                session.setStatus(ViewingSession.CLOSED);
                session.setEndedAt(now);
                // Finalizar con el control cuenta como completada (enunciado)
                session.setCompleted(true);
                session.getEvents().add(SessionEvent.at("finish", now, position));
            }
            default -> throw PlaybackErrors.invalidEvent("Tipo de evento no válido");
        }

        if (session.getLastPositionSec() >= COMPLETION_RATIO * duration) {
            session.setCompleted(true);
        }
        session.setLastActionAt(now);
        trimEvents(session.getEvents());

        saveIfUnchanged(session, previousActionAt);
        progress.record(session, now);
        return SessionResponse.from(session);
    }

    /**
     * Cierra las sesiones abiertas del perfil. Conserva lo ya contado como visto; endedAt es la
     * última acción registrada, no el momento del cierre.
     */
    private void closeOpenSessions(Integer profileId) {
        Query open = new Query(Criteria.where("profileId").is(profileId).and("status").is(ViewingSession.OPEN));
        open.fields().include("lastActionAt");
        List<ViewingSession> list = mongo.find(open, ViewingSession.class);
        for (ViewingSession s : list) {
            mongo.updateFirst(
                new Query(Criteria.where("_id").is(s.getId()).and("status").is(ViewingSession.OPEN)),
                new Update().set("status", ViewingSession.CLOSED)
                            .set("endedAt", s.getLastActionAt())
                            .set("playState.playing", false),
                ViewingSession.class);
        }
    }

    /**
     * Guarda la sesión solo si nadie la modificó desde que se leyó (control optimista con
     * lastActionAt). Si dos eventos llegan a la vez, el segundo recibe 409 CONCURRENT_UPDATE.
     */
    private void saveIfUnchanged(ViewingSession session, Instant previousActionAt) {
        Query unchanged = new Query(Criteria.where("_id").is(session.getId())
            .and("status").is(ViewingSession.OPEN)
            .and("lastActionAt").is(previousActionAt));
        if (mongo.findAndReplace(unchanged, session) == null) {
            throw PlaybackErrors.concurrentUpdate();
        }
    }

    private static void addWatched(ViewingSession session, int position) {
        int delta = position - session.getPlayState().getSegmentStartSec();
        if (delta > 0) {
            session.setWatchedSec(session.getWatchedSec() + delta);
        }
    }

    /** El validador acepta hasta 200 eventos: se conserva el de inicio y los más recientes. */
    private static void trimEvents(List<SessionEvent> events) {
        while (events.size() > ViewingSession.MAX_EVENTS) {
            events.remove(1);
        }
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
