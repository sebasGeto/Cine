package com.cineconecta.catalogo;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.cineconecta.catalogo.dto.ContentDetailResponse;
import com.cineconecta.catalogo.dto.ContentSummaryResponse;
import com.cineconecta.catalogo.dto.PageResponse;
import com.cineconecta.catalogo.dto.SeasonResponse;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo que ve un perfil: solo publicados y con clasificación permitida. Quien llama ya
 * verificó el perfil y la suscripción, y pasa la clasificación máxima del perfil.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ContentRepository contents;
    private final ContentStore store;

    /** RF-21. */
    PageResponse<ContentSummaryResponse> search(CatalogFilter filter, int maxAgeRating, PageRange range) {
        return PageResponse.from(store.search(filter, maxAgeRating, range), ContentSummaryResponse::from);
    }

    /** RF-22. */
    public ContentDetailResponse detail(ObjectId contentId, int maxAgeRating) {
        return ContentDetailResponse.from(requireVisible(contentId, maxAgeRating));
    }

    /** RF-23. */
    public List<SeasonResponse> seasons(ObjectId contentId, int maxAgeRating) {
        Content content = requireVisible(contentId, maxAgeRating);
        if (!content.isSeries()) {
            throw CatalogErrors.notASeries();
        }
        return SeasonResponse.sorted(content.getSeasons());
    }

    /**
     * Contrato para la reproducción (RF-27): lo que se va a reproducir, si el perfil puede verlo.
     *
     * @param episodeIdOrNull episodio; obligatorio en las series y sin efecto en los demás tipos
     * @throws com.cineconecta.common.ApiException 404 CONTENT_NOT_AVAILABLE si no existe o no está
     *         publicado; 403 AGE_RATING_NOT_ALLOWED si supera la clasificación del perfil;
     *         400 EPISODE_REQUIRED si es serie sin episodio; 404 EPISODE_NOT_FOUND si el episodio
     *         no es de esa serie
     */
    public PlayableItem requirePlayable(ObjectId contentId, ObjectId episodeIdOrNull, int maxAgeRating) {
        Content content = requireVisible(contentId, maxAgeRating);
        if (!content.isSeries()) {
            return new PlayableItem(content.getId(), content.getType(), null, null, null,
                    content.getDurationSec());
        }
        if (episodeIdOrNull == null) {
            throw CatalogErrors.episodeRequired();
        }
        for (Season season : content.getSeasons()) {
            for (Episode episode : season.getEpisodes()) {
                if (episodeIdOrNull.equals(episode.getEpisodeId())) {
                    return new PlayableItem(content.getId(), content.getType(), episode.getEpisodeId(),
                            season.getNumber(), episode.getNumber(), episode.getDurationSec());
                }
            }
        }
        throw CatalogErrors.episodeNotFound();
    }

    private Content requireVisible(ObjectId contentId, int maxAgeRating) {
        Content content = contents.findById(contentId)
                .filter(Content::isPublished)
                .orElseThrow(CatalogErrors::contentNotAvailable);
        if (content.getAgeRating() > maxAgeRating) {
            throw CatalogErrors.ageRatingNotAllowed();
        }
        return content;
    }
}
