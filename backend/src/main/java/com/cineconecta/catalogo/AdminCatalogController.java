package com.cineconecta.catalogo;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.catalogo.dto.AdminContentResponse;
import com.cineconecta.catalogo.dto.AdminContentSummaryResponse;
import com.cineconecta.catalogo.dto.ContentRequest;
import com.cineconecta.catalogo.dto.EpisodeRequest;
import com.cineconecta.catalogo.dto.EpisodeUpdateRequest;
import com.cineconecta.catalogo.dto.PageResponse;
import com.cineconecta.catalogo.dto.SeasonRequest;
import com.cineconecta.catalogo.dto.SeasonUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Gestión del catálogo. Solo ADMIN (por la ruta /api/admin). No hay DELETE: los contenidos se retiran. */
@RestController
@RequestMapping("/api/admin/contents")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService catalog;

    /** RF-20. */
    @GetMapping
    public PageResponse<AdminContentSummaryResponse> list(@RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String title,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "12") int size) {
        return catalog.list(CatalogFilter.admin(title, type, status), new PageRange(page, size));
    }

    /** Contenido completo, en cualquier estado: lo necesita el formulario de edición (RF-15). */
    @GetMapping("/{id}")
    public AdminContentResponse get(@PathVariable ObjectId id) {
        return catalog.get(id);
    }

    /** RF-14. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContentResponse create(@Valid @RequestBody ContentRequest request) {
        return catalog.create(request);
    }

    /** RF-15. */
    @PutMapping("/{id}")
    public AdminContentResponse update(@PathVariable ObjectId id, @Valid @RequestBody ContentRequest request) {
        return catalog.update(id, request);
    }

    /** RF-18. */
    @PatchMapping("/{id}/publish")
    public AdminContentResponse publish(@PathVariable ObjectId id) {
        return catalog.publish(id);
    }

    /** RF-19. */
    @PatchMapping("/{id}/retire")
    public AdminContentResponse retire(@PathVariable ObjectId id) {
        return catalog.retire(id);
    }

    /** RF-16. */
    @PostMapping("/{id}/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContentResponse addSeason(@PathVariable ObjectId id, @Valid @RequestBody SeasonRequest request) {
        return catalog.addSeason(id, request);
    }

    /** RF-16. */
    @PutMapping("/{id}/seasons/{season}")
    public AdminContentResponse updateSeason(@PathVariable ObjectId id, @PathVariable int season,
                                             @Valid @RequestBody SeasonUpdateRequest request) {
        return catalog.updateSeason(id, season, request);
    }

    /** RF-17. */
    @PostMapping("/{id}/seasons/{season}/episodes")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContentResponse addEpisode(@PathVariable ObjectId id, @PathVariable int season,
                                           @Valid @RequestBody EpisodeRequest request) {
        return catalog.addEpisode(id, season, request);
    }

    /** RF-17. */
    @PutMapping("/{id}/seasons/{season}/episodes/{episode}")
    public AdminContentResponse updateEpisode(@PathVariable ObjectId id, @PathVariable int season,
                                              @PathVariable int episode,
                                              @Valid @RequestBody EpisodeUpdateRequest request) {
        return catalog.updateEpisode(id, season, episode, request);
    }
}
