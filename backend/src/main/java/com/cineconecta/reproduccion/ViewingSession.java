package com.cineconecta.reproduccion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una sesión de reproducción simulada (colección viewingSessions, RI-11).
 *
 * Historial que solo crece: accountId, subscriptionId, planId, contentType y durationSec se copian
 * al iniciar y no cambian. El informe de consumo (RF-36) agrupa por planId y contentType y suma
 * watchedSec, así que esas copias deben quedar tal como estaban al iniciar.
 *
 * Tiempos en segundos. watchedSec no incluye los saltos con "avanzar" (RF-29).
 */
@Document(ViewingSession.COLLECTION)
@Getter
@Setter
@NoArgsConstructor
public class ViewingSession {

    public static final String COLLECTION = "viewingSessions";

    public static final String OPEN = "open";
    public static final String CLOSED = "closed";
    public static final String ABANDONED = "abandoned";

    /** Máximo de eventos que acepta el validador de la colección. */
    public static final int MAX_EVENTS = 200;

    @Id
    private ObjectId id;

    private Integer accountId;
    private Integer profileId;
    private Integer subscriptionId;
    private Integer planId;

    private ObjectId contentId;
    private String contentType;          // movie | series | documentary
    private ObjectId episodeId;          // solo series
    private Integer seasonNumber;        // solo series
    private Integer episodeNumber;       // solo series
    private Integer durationSec;

    private Instant startedAt;
    private Instant endedAt;             // no se guarda mientras está abierta
    private String status;               // open | closed | abandoned

    private Integer watchedSec;
    private Integer lastPositionSec;
    private Boolean completed;
    private Instant lastActionAt;

    private PlayState playState;
    private List<SessionEvent> events = new ArrayList<>();

    public boolean isOpen() {
        return OPEN.equals(status);
    }

    /** Estado del reproductor: si está sonando y desde qué posición empezó el tramo actual. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayState {
        private Boolean playing;
        private Integer segmentStartSec;
    }

    /** Evento del reproductor. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionEvent {
        private String type;             // start | pause | resume | seek | finish
        private Instant at;
        private Integer positionSec;     // start, pause, resume, finish
        private Integer fromSec;         // solo seek
        private Integer toSec;           // solo seek

        static SessionEvent at(String type, Instant at, int positionSec) {
            return new SessionEvent(type, at, positionSec, null, null);
        }

        static SessionEvent seek(Instant at, int fromSec, int toSec) {
            return new SessionEvent("seek", at, null, fromSec, toSec);
        }
    }
}
