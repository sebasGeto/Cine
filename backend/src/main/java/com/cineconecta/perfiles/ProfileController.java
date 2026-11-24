package com.cineconecta.perfiles;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.perfiles.dto.ProfileRequest;
import com.cineconecta.perfiles.dto.ProfileResponse;
import com.cineconecta.security.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Perfiles de la cuenta autenticada. Solo CLIENT; la cuenta sale del token. */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileAccessService profileAccess;
    private final CurrentUser currentUser;

    /** RF-09. */
    @GetMapping
    public List<ProfileResponse> list() {
        return profileService.list(currentUser.accountId());
    }

    /** RF-05. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(@Valid @RequestBody ProfileRequest request) {
        return profileService.create(currentUser.accountId(), request);
    }

    /** RF-06. */
    @PutMapping("/{id}")
    public ProfileResponse update(@PathVariable Integer id, @Valid @RequestBody ProfileRequest request) {
        return profileService.update(currentUser.accountId(), id, request);
    }

    /** RF-07. */
    @PatchMapping("/{id}/deactivate")
    public ProfileResponse deactivate(@PathVariable Integer id) {
        return profileService.deactivate(currentUser.accountId(), id);
    }

    /** RF-08. */
    @PatchMapping("/{id}/activate")
    public ProfileResponse activate(@PathVariable Integer id) {
        return profileService.activate(currentUser.accountId(), id);
    }

    /** RF-10. El frontend guarda el perfil y lo envía en X-Profile-Id en las siguientes peticiones. */
    @PostMapping("/{id}/select")
    public ActiveProfile select(@PathVariable Integer id) {
        return profileAccess.verify(currentUser.accountId(), id);
    }
}
