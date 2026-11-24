package com.cineconecta.catalogo;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.catalogo.dto.ContentDetailResponse;
import com.cineconecta.catalogo.dto.ContentSummaryResponse;
import com.cineconecta.catalogo.dto.PageResponse;
import com.cineconecta.catalogo.dto.SeasonResponse;
import com.cineconecta.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo para los clientes. Solo CLIENT, con el perfil en X-Profile-Id: antes de todo se
 * verifica que sea un perfil activo de la cuenta del token y que haya suscripción vigente.
 */
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class CatalogController {

    private static final String PROFILE_HEADER = "X-Profile-Id";

    private final CatalogService catalog;
    private final CatalogAccess access;
    private final CurrentUser currentUser;

    /** RF-21. */
    @GetMapping
    public PageResponse<ContentSummaryResponse> search(@RequestHeader(PROFILE_HEADER) Integer profileId,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String genre,
                                                       @RequestParam(required = false) String language,
                                                       @RequestParam(required = false) Integer year,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        int maxAgeRating = maxAgeRating(profileId);
        return catalog.search(CatalogFilter.published(q, type, genre, language, year), maxAgeRating,
                new PageRange(page, size));
    }

    /** RF-22. */
    @GetMapping("/{id}")
    public ContentDetailResponse detail(@RequestHeader(PROFILE_HEADER) Integer profileId, @PathVariable ObjectId id) {
        return catalog.detail(id, maxAgeRating(profileId));
    }

    /** RF-23. */
    @GetMapping("/{id}/seasons")
    public List<SeasonResponse> seasons(@RequestHeader(PROFILE_HEADER) Integer profileId, @PathVariable ObjectId id) {
        return catalog.seasons(id, maxAgeRating(profileId));
    }

    private int maxAgeRating(Integer profileId) {
        return access.requireMaxAgeRating(currentUser.accountId(), profileId);
    }
}
