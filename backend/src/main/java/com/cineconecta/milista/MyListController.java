package com.cineconecta.milista;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.milista.dto.AddToMyListRequest;
import com.cineconecta.milista.dto.MyListEntry;

import jakarta.validation.Valid;

/**
 * Mi lista del perfil con el que navega el cliente. La cuenta sale del token y el perfil
 * del encabezado X-Profile-Id (lo elige el cliente en RF-10).
 *
 *   GET    /api/my-list                RF-26  → 200
 *   POST   /api/my-list  {contentId}   RF-24  → 201  (409 ALREADY_IN_LIST si ya estaba)
 *   DELETE /api/my-list/{contentId}    RF-25  → 204
 */
@RestController
@RequestMapping("/api/my-list")
@Validated
public class MyListController {

    private final MyListService service;

    public MyListController(MyListService service) {
        this.service = service;
    }

    private static final String PROFILE_HEADER = "X-Profile-Id";

    @GetMapping
    public List<MyListEntry> list(@RequestHeader(PROFILE_HEADER) Integer profileId) {
        return service.list(profileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyListEntry add(@RequestHeader(PROFILE_HEADER) Integer profileId,
                           @Valid @RequestBody AddToMyListRequest request) {
        return service.add(profileId, request.contentId());
    }

    @DeleteMapping("/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader(PROFILE_HEADER) Integer profileId, @PathVariable String contentId) {
        service.remove(profileId, contentId);
    }
}
