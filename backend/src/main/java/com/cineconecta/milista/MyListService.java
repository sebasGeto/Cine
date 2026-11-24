package com.cineconecta.milista;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cineconecta.acceso.ActiveProfile;
import com.cineconecta.acceso.ProfileGate;
import com.cineconecta.common.ApiException;
import com.cineconecta.milista.dto.MyListEntry;

/**
 * Mi lista (RF-24 a RF-26). Todo vive en MongoDB; el perfil se valida contra PostgreSQL.
 *
 * - Agregar: solo contenidos publicados y permitidos para el perfil, sin duplicados.
 * - Quitar: siempre se puede, aunque el contenido ya esté retirado.
 * - Ver: muestra todos los guardados y marca los retirados (el enunciado pide distinguirlos).
 */
@Service
public class MyListService {

    private final MyListRepository repository;
    private final MongoTemplate mongo;
    private final ProfileGate profileGate;

    public MyListService(MyListRepository repository, MongoTemplate mongo, ProfileGate profileGate) {
        this.repository = repository;
        this.mongo = mongo;
        this.profileGate = profileGate;
    }

    /** RF-26: lista del perfil, del más reciente al más antiguo. */
    public List<MyListEntry> list(int profileId) {
        ActiveProfile profile = profileGate.requireOwnActiveProfile(profileId);
        List<MyListItem> items = repository.findByProfileIdOrderByAddedAtDesc(profile.profileId());
        if (items.isEmpty()) {
            return List.of();
        }

        // Una sola consulta al catálogo para todos los contenidos de la lista
        List<ObjectId> ids = items.stream().map(MyListItem::getContentId).toList();
        Map<ObjectId, ContentSummary> contents = findContents(ids).stream()
            .collect(Collectors.toMap(ContentSummary::getId, Function.identity()));

        return items.stream()
            .map(item -> toEntry(item, contents.get(item.getContentId()), profile))
            .toList();
    }

    /** RF-24: agrega un contenido a la lista del perfil. */
    public MyListEntry add(int profileId, String contentIdText) {
        ActiveProfile profile = profileGate.requireOwnActiveProfile(profileId);
        ObjectId contentId = parseId(contentIdText);

        ContentSummary content = findContents(List.of(contentId)).stream().findFirst().orElse(null);
        // Un borrador o un id inexistente se responde igual: el cliente no debe saber que existe
        if (content == null || ContentSummary.isDraft(content)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "El contenido no existe");
        }
        if (ContentSummary.RETIRED.equals(content.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_RETIRED", "El contenido fue retirado del catálogo");
        }
        if (!profile.allows(content.getAgeRating())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AGE_RATING_NOT_ALLOWED",
                "La clasificación del contenido no está permitida para este perfil");
        }

        MyListItem item = new MyListItem(null, profile.accountId(), profile.profileId(), contentId, Instant.now());
        try {
            repository.insert(item);
        } catch (DuplicateKeyException e) {
            // Índice único ux_mylist_profile_content: también protege ante dos clics simultáneos
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_IN_MY_LIST", "El contenido ya está en Mi lista");
        }
        return toEntry(item, content, profile);
    }

    /** RF-25: quita un contenido de la lista del perfil. */
    public void remove(int profileId, String contentIdText) {
        ActiveProfile profile = profileGate.requireOwnActiveProfile(profileId);
        long deleted = repository.deleteByProfileIdAndContentId(profile.profileId(), parseId(contentIdText));
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_IN_MY_LIST", "El contenido no está en Mi lista");
        }
    }

    private List<ContentSummary> findContents(List<ObjectId> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        query.fields().include("type", "title", "releaseYear", "ageRating", "coverUrl", "status");
        return mongo.find(query, ContentSummary.class, ContentSummary.COLLECTION);
    }

    private static MyListEntry toEntry(MyListItem item, ContentSummary c, ActiveProfile profile) {
        if (c == null) {
            // El contenido ya no está en el catálogo: se trata como retirado
            return new MyListEntry(item.getContentId().toHexString(), null, null, null, null, null,
                false, true, false, item.getAddedAt());
        }
        boolean retired = ContentSummary.RETIRED.equals(c.getStatus());
        boolean published = ContentSummary.PUBLISHED.equals(c.getStatus());
        boolean allowed = c.getAgeRating() != null && profile.allows(c.getAgeRating());
        return new MyListEntry(c.getId().toHexString(), c.getTitle(), c.getType(), c.getReleaseYear(),
            c.getAgeRating(), c.getCoverUrl(), published && allowed, retired, allowed, item.getAddedAt());
    }

    private static ObjectId parseId(String text) {
        if (text == null || !ObjectId.isValid(text)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "El valor de 'contentId' no es válido");
        }
        return new ObjectId(text);
    }
}
