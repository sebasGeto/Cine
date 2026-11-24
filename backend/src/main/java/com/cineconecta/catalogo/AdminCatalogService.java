package com.cineconecta.catalogo;

import java.time.Instant;
import java.util.function.Supplier;

import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cineconecta.catalogo.dto.AdminContentResponse;
import com.cineconecta.catalogo.dto.AdminContentSummaryResponse;
import com.cineconecta.catalogo.dto.ContentRequest;
import com.cineconecta.catalogo.dto.EpisodeRequest;
import com.cineconecta.catalogo.dto.EpisodeUpdateRequest;
import com.cineconecta.catalogo.dto.PageResponse;
import com.cineconecta.catalogo.dto.SeasonRequest;
import com.cineconecta.catalogo.dto.SeasonUpdateRequest;
import com.cineconecta.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Gestión del catálogo (RF-14 a RF-20). Solo MongoDB: ninguna operación toca PostgreSQL.
 *
 * Primero se intenta la escritura condicional y solo si no tuvo efecto se lee el documento para
 * saber por qué. Al revés (leer y después escribir) otra petición podría cambiarlo en el medio.
 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final ContentRepository contents;
    private final ContentStore store;

    /** RF-20. */
    PageResponse<AdminContentSummaryResponse> list(CatalogFilter filter, PageRange range) {
        return PageResponse.from(store.list(filter, range), AdminContentSummaryResponse::from);
    }

    public AdminContentResponse get(ObjectId id) {
        return AdminContentResponse.from(require(id));
    }

    /** RF-14. */
    public AdminContentResponse create(ContentRequest request) {
        if (ContentType.SERIES.equals(request.type())
                && (request.seasons() == null || request.seasons().isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "seasons: debe tener al menos una en el tipo series");
        }
        Content content = ContentMapper.newContent(request, Instant.now());
        boolean emptySeason = content.isSeries()
                && content.getSeasons().stream().anyMatch(s -> s.getEpisodes().isEmpty());
        return AdminContentResponse.from(withEmptySeasonCheck(emptySeason, () -> contents.insert(content)));
    }

    /** RF-15. */
    public AdminContentResponse update(ObjectId id, ContentRequest request) {
        if (!store.updateFields(id, request.type(), ContentMapper.updatedFields(request), Instant.now())) {
            require(id);
            throw CatalogErrors.typeCannotChange();
        }
        return get(id);
    }

    /** RF-18. Publicar lo ya publicado no cambia nada. */
    public AdminContentResponse publish(ObjectId id) {
        store.publish(id, Instant.now());
        return get(id);
    }

    /** RF-19. Retirar lo ya retirado no cambia nada. */
    public AdminContentResponse retire(ObjectId id) {
        store.retire(id, Instant.now());
        return get(id);
    }

    /** RF-16. */
    public AdminContentResponse addSeason(ObjectId id, SeasonRequest request) {
        Season season = ContentMapper.newSeason(request);
        boolean added = withEmptySeasonCheck(season.getEpisodes().isEmpty(),
                () -> store.addSeason(id, season, Instant.now()));
        if (!added) {
            requireSeries(id);
            throw CatalogErrors.seasonNumberExists();
        }
        return get(id);
    }

    /** RF-16. */
    public AdminContentResponse updateSeason(ObjectId id, int seasonNumber, SeasonUpdateRequest request) {
        if (!store.updateSeason(id, seasonNumber, request.description().trim(), Instant.now())) {
            requireSeries(id);
            throw CatalogErrors.seasonNotFound();
        }
        return get(id);
    }

    /** RF-17. */
    public AdminContentResponse addEpisode(ObjectId id, int seasonNumber, EpisodeRequest request) {
        if (!store.addEpisode(id, seasonNumber, ContentMapper.newEpisode(request), Instant.now())) {
            requireSeason(requireSeries(id), seasonNumber);
            throw CatalogErrors.episodeNumberExists();
        }
        return get(id);
    }

    /** RF-17. */
    public AdminContentResponse updateEpisode(ObjectId id, int seasonNumber, int episodeNumber,
                                              EpisodeUpdateRequest request) {
        if (!store.updateEpisode(id, seasonNumber, episodeNumber, request.title().trim(),
                request.synopsis().trim(), request.durationSec(), Instant.now())) {
            requireSeason(requireSeries(id), seasonNumber);
            throw CatalogErrors.episodeNotFound();
        }
        return get(id);
    }

    // ------------------------------------------------------------------

    private Content require(ObjectId id) {
        return contents.findById(id).orElseThrow(CatalogErrors::contentNotFound);
    }

    private Content requireSeries(ObjectId id) {
        Content content = require(id);
        if (!content.isSeries()) {
            throw CatalogErrors.notASeries();
        }
        return content;
    }

    private static void requireSeason(Content series, int seasonNumber) {
        if (series.getSeasons().stream().noneMatch(s -> s.getNumber() == seasonNumber)) {
            throw CatalogErrors.seasonNotFound();
        }
    }

    /**
     * ux_contents_episode_id es único y una temporada sin episodios cuenta como episodeId null,
     * así que MongoDB solo admite una serie con temporadas vacías a la vez. Los episodeId los
     * genera el servidor: con una temporada vacía, ese es el único duplicado posible. Aquí se
     * explica el error en lugar de responder "el registro ya existe".
     */
    private static <T> T withEmptySeasonCheck(boolean emptySeason, Supplier<T> write) {
        try {
            return write.get();
        } catch (DuplicateKeyException e) {
            throw emptySeason ? CatalogErrors.emptySeasonConflict() : e;
        }
    }
}
