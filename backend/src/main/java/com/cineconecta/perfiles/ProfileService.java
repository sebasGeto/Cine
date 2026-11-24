package com.cineconecta.perfiles;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cineconecta.common.ApiException;
import com.cineconecta.perfiles.dto.ProfileRequest;
import com.cineconecta.perfiles.dto.ProfileResponse;

import lombok.RequiredArgsConstructor;

/**
 * Perfiles de la cuenta autenticada.
 *
 * El cupo del plan y la suscripción vigente NO se validan aquí: los exige el trigger
 * trg_profiles_limit al crear o reactivar (con bloqueo de la cuenta, así dos peticiones
 * simultáneas no superan el cupo). El nombre repetido lo rechaza el índice ux_profiles_name_ci.
 * Sus errores salen del método (rollback) y los traduce el GlobalExceptionHandler:
 * 403 NO_ACTIVE_SUBSCRIPTION, 409 PROFILE_LIMIT_REACHED, 409 PROFILE_NAME_EXISTS.
 *
 * saveAndFlush hace que el error ocurra dentro del método y no al confirmar la transacción.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profiles;

    /** RF-09. Activos e inactivos. */
    @Transactional(readOnly = true)
    public List<ProfileResponse> list(Integer accountId) {
        return profiles.findByAccountIdOrderByProfileId(accountId).stream().map(ProfileResponse::from).toList();
    }

    /** RF-05. */
    @Transactional
    public ProfileResponse create(Integer accountId, ProfileRequest request) {
        Profile profile = new Profile();
        profile.setAccountId(accountId);
        profile.setName(request.name());
        profile.setMaxAgeRating(request.maxAgeRating());
        profile.setIsActive(Profile.ACTIVE);
        return ProfileResponse.from(profiles.saveAndFlush(profile));
    }

    /** RF-06. */
    @Transactional
    public ProfileResponse update(Integer accountId, Integer profileId, ProfileRequest request) {
        Profile profile = own(accountId, profileId);
        profile.setName(request.name());
        profile.setMaxAgeRating(request.maxAgeRating());
        return ProfileResponse.from(profiles.saveAndFlush(profile));
    }

    /** RF-07. No se borra nada: el historial de visualización sigue apuntando al perfil. */
    @Transactional
    public ProfileResponse deactivate(Integer accountId, Integer profileId) {
        return setActive(accountId, profileId, Profile.INACTIVE);
    }

    /** RF-08. El trigger valida suscripción vigente y cupo. */
    @Transactional
    public ProfileResponse activate(Integer accountId, Integer profileId) {
        return setActive(accountId, profileId, Profile.ACTIVE);
    }

    private ProfileResponse setActive(Integer accountId, Integer profileId, String value) {
        Profile profile = own(accountId, profileId);
        profile.setIsActive(value);
        return ProfileResponse.from(profiles.saveAndFlush(profile));
    }

    /** Un perfil de otra cuenta se responde igual que uno inexistente, para no revelar que existe. */
    private Profile own(Integer accountId, Integer profileId) {
        return profiles.findByProfileIdAndAccountId(profileId, accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "El perfil no existe"));
    }
}
