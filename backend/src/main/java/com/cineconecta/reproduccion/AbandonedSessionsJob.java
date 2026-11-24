package com.cineconecta.reproduccion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * RF-32 (opcional): cierra las sesiones que quedaron abiertas sin actividad (el usuario cerró la
 * pestaña sin finalizar). Quedan con status "abandoned" y endedAt = última acción; conservan el
 * tiempo visto ya contado. La consulta usa el índice parcial ix_sessions_open.
 *
 * Configurable con app.playback.abandon-after-minutes (por defecto 240) y
 * app.playback.abandon-check-ms (por defecto cada 5 minutos).
 */
@Configuration
@EnableScheduling
public class AbandonedSessionsJob {

    private static final Logger log = LoggerFactory.getLogger(AbandonedSessionsJob.class);

    private final MongoTemplate mongo;
    private final Duration abandonAfter;

    public AbandonedSessionsJob(MongoTemplate mongo,
                                @Value("${app.playback.abandon-after-minutes:240}") long abandonAfterMinutes) {
        this.mongo = mongo;
        this.abandonAfter = Duration.ofMinutes(abandonAfterMinutes);
    }

    @Scheduled(fixedDelayString = "${app.playback.abandon-check-ms:300000}",
               initialDelayString = "${app.playback.abandon-check-ms:300000}")
    public void closeAbandoned() {
        try {
            int closed = closeIdleSince(Instant.now().minus(abandonAfter));
            if (closed > 0) {
                log.info("Sesiones de reproducción abandonadas cerradas: {}", closed);
            }
        } catch (RuntimeException e) {
            // Si MongoDB no está disponible, se intenta en la siguiente ejecución
            log.warn("No se pudieron cerrar las sesiones abandonadas: {}", e.getMessage());
        }
    }

    int closeIdleSince(Instant limit) {
        Query idle = new Query(Criteria.where("status").is(ViewingSession.OPEN).and("lastActionAt").lt(limit));
        idle.fields().include("lastActionAt");
        List<ViewingSession> list = mongo.find(idle, ViewingSession.class);
        int closed = 0;
        for (ViewingSession s : list) {
            // Condición sobre lastActionAt: si llegó un evento mientras tanto, no se toca
            Query unchanged = new Query(Criteria.where("_id").is(s.getId())
                .and("status").is(ViewingSession.OPEN)
                .and("lastActionAt").is(s.getLastActionAt()));
            Update update = new Update()
                .set("status", ViewingSession.ABANDONED)
                .set("endedAt", s.getLastActionAt())
                .set("playState.playing", false);
            closed += (int) mongo.updateFirst(unchanged, update, ViewingSession.class).getModifiedCount();
        }
        return closed;
    }
}
