package com.cineconecta.reproduccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.cineconecta.catalogo.CatalogService;
import com.cineconecta.common.ApiException;
import com.cineconecta.perfiles.ActiveProfile;
import com.cineconecta.perfiles.ProfileAccessService;
import com.cineconecta.reproduccion.ViewingSession.PlayState;
import com.cineconecta.reproduccion.dto.PlaybackEventRequest;
import com.cineconecta.reproduccion.dto.SessionResponse;
import com.cineconecta.security.CurrentUser;
import com.cineconecta.suscripciones.SubscriptionService;

/** Cálculo del tiempo visto, saltos, pausa y 90 % de la reproducción simulada (sin bases de datos). */
class PlaybackServiceTest {

    private static final int ACCOUNT = 1;
    private static final int PROFILE = 1;

    private final ViewingSessionRepository sessions = mock(ViewingSessionRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ProfileAccessService profileAccess = mock(ProfileAccessService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);

    private PlaybackService service;
    private ViewingSession session;

    @BeforeEach
    void setUp() {
        service = new PlaybackService(sessions, mongo, profileAccess, mock(SubscriptionService.class),
            mock(CatalogService.class), mock(ProgressService.class), currentUser);

        when(currentUser.accountId()).thenReturn(ACCOUNT);
        when(profileAccess.verify(ACCOUNT, PROFILE)).thenReturn(new ActiveProfile(PROFILE, ACCOUNT, "Ana", 18));
        when(mongo.findAndReplace(any(Query.class), any(ViewingSession.class))).thenAnswer(inv -> inv.getArgument(1));

        // Película de 1000 s, sonando desde el segundo 0
        session = new ViewingSession();
        session.setId(new ObjectId());
        session.setAccountId(ACCOUNT);
        session.setProfileId(PROFILE);
        session.setContentId(new ObjectId());
        session.setContentType("movie");
        session.setDurationSec(1000);
        session.setStatus(ViewingSession.OPEN);
        session.setWatchedSec(0);
        session.setLastPositionSec(0);
        session.setCompleted(false);
        session.setStartedAt(Instant.now());
        session.setLastActionAt(Instant.now());
        session.setPlayState(new PlayState(true, 0));
        when(sessions.findByIdAndProfileId(eq(session.getId()), eq(PROFILE))).thenReturn(Optional.of(session));
    }

    private SessionResponse send(String type, int position, Integer to) {
        return service.event(PROFILE, session.getId().toHexString(), new PlaybackEventRequest(type, position, to));
    }

    @Test
    void pauseCountsTheWatchedSegment() {
        SessionResponse r = send("pause", 120, null);

        assertThat(r.watchedSec()).isEqualTo(120);
        assertThat(r.positionSec()).isEqualTo(120);
        assertThat(r.playing()).isFalse();
    }

    @Test
    void seekDoesNotCountTheSkippedPart() {
        send("seek", 100, 600);              // vio 100 s y saltó hasta 600
        SessionResponse r = send("pause", 650, null);

        assertThat(r.watchedSec()).isEqualTo(150);   // 100 + 50, sin los 500 saltados
        assertThat(r.positionSec()).isEqualTo(650);
    }

    @Test
    void reachingNinetyPercentMarksCompleted() {
        SessionResponse r = send("pause", 900, null);

        assertThat(r.completed()).isTrue();
        assertThat(r.status()).isEqualTo(ViewingSession.OPEN);
    }

    @Test
    void finishClosesAndMarksCompleted() {
        SessionResponse r = send("finish", 300, null);

        assertThat(r.status()).isEqualTo(ViewingSession.CLOSED);
        assertThat(r.completed()).isTrue();
        assertThat(r.watchedSec()).isEqualTo(300);
        assertThat(r.endedAt()).isNotNull();
    }

    @Test
    void positionsBeyondTheDurationAreClamped() {
        SessionResponse r = send("pause", 5000, null);

        assertThat(r.positionSec()).isEqualTo(1000);
        assertThat(r.watchedSec()).isEqualTo(1000);
    }

    @Test
    void pausingTwiceIsRejected() {
        send("pause", 50, null);

        assertThatThrownBy(() -> send("pause", 60, null))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("code", "INVALID_PLAYBACK_EVENT");
    }

    @Test
    void eventsOnAClosedSessionAreRejected() {
        session.setStatus(ViewingSession.CLOSED);

        assertThatThrownBy(() -> send("resume", 0, null))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("code", "SESSION_CLOSED");
    }

    @Test
    void concurrentChangeIsRejected() {
        when(mongo.findAndReplace(any(Query.class), any(ViewingSession.class))).thenReturn(null);

        assertThatThrownBy(() -> send("pause", 10, null))
            .isInstanceOf(ApiException.class)
            .hasFieldOrPropertyWithValue("code", "CONCURRENT_UPDATE");
    }
}
