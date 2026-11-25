package com.cineconecta.reproduccion;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cineconecta.catalogo.Content;
import com.cineconecta.catalogo.ContentStatus;
import com.cineconecta.common.ApiException;
import com.cineconecta.perfiles.ActiveProfile;
import com.cineconecta.perfiles.ProfileAccessService;
import com.cineconecta.reproduccion.dto.ContinueWatchingItem;
import com.cineconecta.reproduccion.dto.HistoryItem;
import com.cineconecta.reproduccion.dto.HistoryPage;
import com.cineconecta.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas de la actividad del perfil: "Continuar viendo" (RF-31) e historial de sesiones.
 */
@Service
@RequiredArgsConstructor
public class ViewingActivityService {

    /** Elementos que muestra "Continuar viendo". */
    static final int CONTINUE_LIMIT = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ViewingProgressRepository progressRepository;
    private final ViewingSessionRepository sessions;
    private final MongoTemplate mongo;
    private final ProfileAccessService profileAccess;
    private final CurrentUser currentUser;

    /**
     * RF-31: contenidos en progreso, del más reciente al más antiguo, con la posición guardada.
     * No aparecen los retirados (el enunciado lo exige) ni los que superan la clasificación actual
     * del perfil. Se leen algunos de más para completar la lista después de ese filtro.
     */
    public List<ContinueWatchingItem> continueWatching(Integer profileId) {
        ActiveProfile profile = profileAccess.verify(currentUser.accountId(), profileId);
        List<ViewingProgress> inProgress = progressRepository
            .findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(profile.profileId(),
                PageRequest.of(0, CONTINUE_LIMIT * 2));
        if (inProgress.isEmpty()) {
            return List.of();
        }

        Map<ObjectId, Content> visible = findContents(
            inProgress.stream().map(ViewingProgress::getContentId).toList(), true);

        return inProgress.stream()
            .filter(p -> {
                Content c = visible.get(p.getContentId());
                return c != null && c.getAgeRating() <= profile.maxAgeRating() && p.getCurrent() != null;
            })
            .limit(CONTINUE_LIMIT)
            .map(p -> toItem(p, visible.get(p.getContentId())))
            .toList();
    }

    /** Historial de sesiones del perfil, de la más reciente a la más antigua. */
    public HistoryPage history(Integer profileId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "page debe ser >= 0 y size entre 1 y " + MAX_PAGE_SIZE);
        }
        ActiveProfile profile = profileAccess.verify(currentUser.accountId(), profileId);
        List<ViewingSession> list = sessions.findByProfileIdOrderByStartedAtDesc(profile.profileId(),
            PageRequest.of(page, size));

        // El historial conserva las referencias aunque el contenido se haya retirado
        Map<ObjectId, Content> titles = findContents(list.stream().map(ViewingSession::getContentId).toList(), false);

        List<HistoryItem> items = list.stream().map(s -> {
            Content c = titles.get(s.getContentId());
            return new HistoryItem(
                s.getId().toHexString(), s.getContentId().toHexString(), c == null ? null : c.getTitle(),
                s.getContentType(), hex(s.getEpisodeId()), s.getSeasonNumber(), s.getEpisodeNumber(),
                s.getStatus(), s.getWatchedSec(), s.getLastPositionSec(), s.getDurationSec(),
                Boolean.TRUE.equals(s.getCompleted()), s.getStartedAt(), s.getEndedAt());
        }).toList();
        return new HistoryPage(items, page, size, sessions.countByProfileId(profile.profileId()));
    }

    /** Una sola consulta al catálogo, solo con los campos que se muestran. */
    private Map<ObjectId, Content> findContents(List<ObjectId> ids, boolean onlyPublished) {
        Criteria criteria = Criteria.where("_id").in(ids);
        if (onlyPublished) {
            criteria = criteria.and("status").is(ContentStatus.PUBLISHED);
        }
        Query query = new Query(criteria);
        query.fields().include("title", "type", "coverUrl", "ageRating", "status");
        return mongo.find(query, Content.class).stream()
            .collect(Collectors.toMap(Content::getId, Function.identity()));
    }

    private static ContinueWatchingItem toItem(ViewingProgress p, Content c) {
        ViewingProgress.Current cur = p.getCurrent();
        int percent = cur.getDurationSec() == null || cur.getDurationSec() == 0 ? 0
            : (int) Math.min(100, Math.round(100.0 * cur.getPositionSec() / cur.getDurationSec()));
        return new ContinueWatchingItem(
            p.getContentId().toHexString(), c.getTitle(), p.getContentType(), c.getCoverUrl(),
            hex(cur.getEpisodeId()), cur.getSeasonNumber(), cur.getEpisodeNumber(),
            cur.getPositionSec(), cur.getDurationSec(), percent, p.getLastActivityAt());
    }

    private static String hex(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}
