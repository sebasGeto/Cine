package com.cineconecta.catalogo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Búsquedas y actualizaciones parciales de contents.
 *
 * Cada actualización es un solo updateOne con su condición en el filtro: MongoDB la evalúa y
 * escribe de forma atómica, así que dos administradores no pueden crear el mismo número de
 * temporada o episodio aunque envíen la petición a la vez. Los métodos devuelven false si
 * ningún documento cumplió la condición.
 */
@Repository
@RequiredArgsConstructor
public class ContentStore {

    private final MongoTemplate mongoTemplate;

    // ------------------------------------------------------------------
    // Lectura
    // ------------------------------------------------------------------

    /**
     * RF-21: búsqueda del cliente, los más recientes primero. El _id desempata para que un
     * contenido no salga en dos páginas. Usa ix_contents_browse, ix_contents_genres,
     * ix_contents_audio o tx_contents_title según los filtros.
     */
    Page<Content> search(CatalogFilter filter, int maxAgeRating, PageRange range) {
        if (filter.matchesNothing()) {
            return Page.empty(range.toPageRequest());
        }
        Query query = query(filter).addCriteria(Criteria.where("ageRating").lte(maxAgeRating));
        return page(query, range, Sort.by(Sort.Order.desc("releaseYear"), Sort.Order.asc("_id")),
                "type", "title", "releaseYear", "ageRating", "coverUrl", "genres");
    }

    /** RF-20: listado del administrador, los últimos creados primero. */
    Page<Content> list(CatalogFilter filter, PageRange range) {
        if (filter.matchesNothing()) {
            return Page.empty(range.toPageRequest());
        }
        return page(query(filter), range, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("_id")),
                "type", "title", "releaseYear", "ageRating", "coverUrl", "genres", "status",
                "publishedAt", "retiredAt", "createdAt", "updatedAt");
    }

    private static Query query(CatalogFilter f) {
        Query query = new Query();
        if (f.title() != null) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(Texts.allWords(f.title())));
        }
        where(query, "status", f.status());
        where(query, "type", f.type());
        where(query, "genres", f.genre());
        where(query, "audioLanguages", f.language());
        where(query, "releaseYear", f.year());
        return query;
    }

    private static void where(Query query, String field, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(field).is(value));
        }
    }

    private Page<Content> page(Query query, PageRange range, Sort sort, String... fields) {
        long total = mongoTemplate.count(query, Content.class);
        if (total == 0) {
            return Page.empty(range.toPageRequest());
        }
        query.fields().include(fields);
        List<Content> items = mongoTemplate.find(query.with(range.toPageRequest()).with(sort), Content.class);
        return new PageImpl<>(items, range.toPageRequest(), total);
    }

    // ------------------------------------------------------------------
    // Contenido
    // ------------------------------------------------------------------

    /** RF-15. Cambia solo los campos recibidos; un valor null quita el campo. No toca _id, type ni status. */
    boolean updateFields(ObjectId id, String type, Map<String, Object> fields, Instant now) {
        Update update = new Update().set("updatedAt", now);
        fields.forEach((field, value) -> {
            if (value == null) {
                update.unset(field);
            } else {
                update.set(field, value);
            }
        });
        return apply(new Query(Criteria.where("_id").is(id).and("type").is(type)), update);
    }

    /** RF-18. Si ya estaba publicado no hace nada: conserva su publishedAt. */
    boolean publish(ObjectId id, Instant now) {
        return apply(new Query(Criteria.where("_id").is(id).and("status").ne(ContentStatus.PUBLISHED)),
                new Update().set("status", ContentStatus.PUBLISHED).set("publishedAt", now)
                        .set("updatedAt", now).unset("retiredAt"));
    }

    /** RF-19. El documento se conserva siempre: Mi lista y las sesiones lo referencian por _id. */
    boolean retire(ObjectId id, Instant now) {
        return apply(new Query(Criteria.where("_id").is(id).and("status").ne(ContentStatus.RETIRED)),
                new Update().set("status", ContentStatus.RETIRED).set("retiredAt", now).set("updatedAt", now));
    }

    // ------------------------------------------------------------------
    // Temporadas y episodios
    // ------------------------------------------------------------------

    /** RF-16. Solo agrega si ninguna temporada tiene ese número. */
    boolean addSeason(ObjectId seriesId, Season season, Instant now) {
        Criteria free = series(seriesId).and("seasons.number").ne(season.getNumber());
        return apply(new Query(free), new Update().push("seasons", season).set("updatedAt", now));
    }

    /** RF-16. El número de la temporada no cambia: identifica la temporada en las sesiones históricas. */
    boolean updateSeason(ObjectId seriesId, int seasonNumber, String description, Instant now) {
        Criteria exists = series(seriesId).and("seasons.number").is(seasonNumber);
        return apply(new Query(exists), new Update()
                .set("seasons.$[s].description", description)
                .set("updatedAt", now)
                .filterArray(Criteria.where("s.number").is(seasonNumber)));
    }

    /**
     * RF-17 (consulta 6.3 de la entrega previa). La condición va también en el filtro y no solo en
     * arrayFilters: si ningún elemento del arreglo coincide, updatedAt no debe cambiar.
     */
    boolean addEpisode(ObjectId seriesId, int seasonNumber, Episode episode, Instant now) {
        Criteria free = series(seriesId).and("seasons").elemMatch(
                Criteria.where("number").is(seasonNumber).and("episodes.number").ne(episode.getNumber()));
        return apply(new Query(free), new Update()
                .push("seasons.$[s].episodes", episode)
                .set("updatedAt", now)
                .filterArray(Criteria.where("s.number").is(seasonNumber)
                        .and("s.episodes.number").ne(episode.getNumber())));
    }

    /** RF-17. No cambia episodeId ni el número: los referencian las sesiones y el progreso. */
    boolean updateEpisode(ObjectId seriesId, int seasonNumber, int episodeNumber,
                          String title, String synopsis, int durationSec, Instant now) {
        Criteria exists = series(seriesId).and("seasons").elemMatch(
                Criteria.where("number").is(seasonNumber).and("episodes.number").is(episodeNumber));
        String path = "seasons.$[s].episodes.$[e].";
        return apply(new Query(exists), new Update()
                .set(path + "title", title)
                .set(path + "synopsis", synopsis)
                .set(path + "durationSec", durationSec)
                .set("updatedAt", now)
                .filterArray(Criteria.where("s.number").is(seasonNumber))
                .filterArray(Criteria.where("e.number").is(episodeNumber)));
    }

    private static Criteria series(ObjectId id) {
        return Criteria.where("_id").is(id).and("type").is(ContentType.SERIES);
    }

    private boolean apply(Query query, Update update) {
        return mongoTemplate.updateFirst(query, update, Content.class).getMatchedCount() > 0;
    }
}
