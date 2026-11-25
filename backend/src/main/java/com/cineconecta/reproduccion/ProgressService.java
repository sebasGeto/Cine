package com.cineconecta.reproduccion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.cineconecta.catalogo.Content;
import com.cineconecta.catalogo.ContentRepository;
import com.cineconecta.catalogo.ContentType;
import com.cineconecta.catalogo.Episode;
import com.cineconecta.catalogo.PlayableItem;
import com.cineconecta.catalogo.Season;
import com.cineconecta.reproduccion.ViewingProgress.Current;
import com.cineconecta.reproduccion.ViewingProgress.EpisodeProgress;

import lombok.RequiredArgsConstructor;

/**
 * Progreso actual de un perfil por contenido (viewingProgress), RF-30 y RF-31.
 *
 * Se actualiza después de cada evento de la sesión. Un solo documento por perfil y contenido
 * (ux_progress_profile_content): si dos peticiones intentan crearlo a la vez, la segunda choca con
 * el índice único y se reintenta sobre el documento que ya existe.
 *
 * Series: guarda el progreso de cada episodio. Al completar un episodio, "Continuar viendo" pasa
 * al siguiente desde el inicio; al completar el último, la serie queda completada.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ViewingProgressRepository progressRepository;
    private final ContentRepository contents;

    /** Posición desde la que se retoma: la guardada, salvo que ya estuviera completado. */
    int resumePosition(Integer profileId, PlayableItem item) {
        Optional<ViewingProgress> saved = progressRepository.findByProfileIdAndContentId(profileId, item.contentId());
        if (saved.isEmpty()) {
            return 0;
        }
        ViewingProgress p = saved.get();
        int position;
        boolean completed;
        if (item.episodeId() == null) {
            position = p.getCurrent() == null ? 0 : p.getCurrent().getPositionSec();
            completed = Boolean.TRUE.equals(p.getCompleted());
        } else {
            Optional<EpisodeProgress> ep = p.episodesOrEmpty().stream()
                .filter(e -> item.episodeId().equals(e.getEpisodeId())).findFirst();
            if (ep.isEmpty()) {
                return 0;
            }
            position = ep.get().getPositionSec();
            completed = Boolean.TRUE.equals(ep.get().getCompleted());
        }
        boolean almostDone = position >= PlaybackService.COMPLETION_RATIO * item.durationSec();
        return completed || almostDone ? 0 : position;
    }

    /** Refleja en viewingProgress el estado de la sesión. */
    void record(ViewingSession session, Instant now) {
        try {
            save(session, now);
        } catch (DuplicateKeyException e) {
            // Otra petición creó el documento al mismo tiempo: se aplica el cambio sobre ese
            save(session, now);
        }
    }

    private void save(ViewingSession s, Instant now) {
        ViewingProgress p = progressRepository.findByProfileIdAndContentId(s.getProfileId(), s.getContentId())
            .orElseGet(() -> newProgress(s));
        p.setLastActivityAt(now);
        boolean sessionCompleted = Boolean.TRUE.equals(s.getCompleted());

        if (!ContentType.SERIES.equals(s.getContentType())) {
            p.setCurrent(new Current(s.getLastPositionSec(), s.getDurationSec(), null, null, null));
            p.setCompleted(sessionCompleted);
        } else {
            updateEpisode(p, s, now, sessionCompleted);
            Optional<NextEpisode> next = sessionCompleted ? nextEpisode(s.getContentId(), s.getEpisodeId()) : Optional.empty();
            if (sessionCompleted && next.isEmpty()) {
                // Terminó el último episodio: la serie sale de "Continuar viendo"
                p.setCurrent(episodeCurrent(s));
                p.setCompleted(true);
            } else if (next.isPresent()) {
                NextEpisode n = next.get();
                p.setCurrent(new Current(0, n.durationSec(), n.episodeId(), n.seasonNumber(), n.episodeNumber()));
                p.setCompleted(false);
            } else {
                p.setCurrent(episodeCurrent(s));
                p.setCompleted(false);
            }
        }
        progressRepository.save(p);
    }

    private static ViewingProgress newProgress(ViewingSession s) {
        ViewingProgress p = new ViewingProgress();
        p.setAccountId(s.getAccountId());
        p.setProfileId(s.getProfileId());
        p.setContentId(s.getContentId());
        p.setContentType(s.getContentType());
        return p;
    }

    private static Current episodeCurrent(ViewingSession s) {
        return new Current(s.getLastPositionSec(), s.getDurationSec(), s.getEpisodeId(),
            s.getSeasonNumber(), s.getEpisodeNumber());
    }

    /** Un episodio completado sigue completado aunque se vuelva a ver. */
    private static void updateEpisode(ViewingProgress p, ViewingSession s, Instant now, boolean completed) {
        List<EpisodeProgress> episodes = p.episodesOrEmpty();
        for (EpisodeProgress e : episodes) {
            if (s.getEpisodeId().equals(e.getEpisodeId())) {
                e.setPositionSec(s.getLastPositionSec());
                e.setCompleted(Boolean.TRUE.equals(e.getCompleted()) || completed);
                e.setUpdatedAt(now);
                return;
            }
        }
        episodes.add(new EpisodeProgress(s.getEpisodeId(), s.getLastPositionSec(), completed, now));
    }

    /** Episodio siguiente en orden de temporada y número, o vacío si era el último. */
    private Optional<NextEpisode> nextEpisode(ObjectId contentId, ObjectId episodeId) {
        Content content = contents.findById(contentId).orElse(null);
        if (content == null || content.getSeasons() == null) {
            return Optional.empty();
        }
        List<NextEpisode> ordered = new ArrayList<>();
        content.getSeasons().stream()
            .sorted(Comparator.comparing(Season::getNumber))
            .filter(season -> season.getEpisodes() != null)
            .forEach(season -> season.getEpisodes().stream()
                .sorted(Comparator.comparing(Episode::getNumber))
                .forEach(e -> ordered.add(new NextEpisode(e.getEpisodeId(), season.getNumber(),
                    e.getNumber(), e.getDurationSec()))));
        for (int i = 0; i < ordered.size() - 1; i++) {
            if (ordered.get(i).episodeId().equals(episodeId)) {
                return Optional.of(ordered.get(i + 1));
            }
        }
        return Optional.empty();
    }

    private record NextEpisode(ObjectId episodeId, Integer seasonNumber, Integer episodeNumber, Integer durationSec) {}
}
