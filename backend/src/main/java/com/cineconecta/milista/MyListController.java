package com.cineconecta.milista;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.milista.dto.AddToMyListRequest;
import com.cineconecta.milista.dto.MyListEntry;

import jakarta.validation.Valid;

/**
 * Mi lista de un perfil (solo clientes; la cuenta sale del token).
 *
 *   GET    /api/profiles/{profileId}/my-list               RF-26
 *   POST   /api/profiles/{profileId}/my-list  {contentId}  RF-24  → 201
 *   DELETE /api/profiles/{profileId}/my-list/{contentId}   RF-25  → 204
 */
@RestController
@RequestMapping("/api/profiles/{profileId}/my-list")
@Validated
public class MyListController {

    private final MyListService service;

    public MyListController(MyListService service) {
        this.service = service;
    }

    @GetMapping
    public List<MyListEntry> list(@PathVariable int profileId) {
        return service.list(profileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyListEntry add(@PathVariable int profileId, @Valid @RequestBody AddToMyListRequest request) {
        return service.add(profileId, request.contentId());
    }

    @DeleteMapping("/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable int profileId, @PathVariable String contentId) {
        service.remove(profileId, contentId);
    }
}
