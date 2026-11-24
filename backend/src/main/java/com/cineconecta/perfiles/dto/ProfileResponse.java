package com.cineconecta.perfiles.dto;

import com.cineconecta.perfiles.Profile;

public record ProfileResponse(Integer profileId, String name, Integer maxAgeRating, boolean isActive) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getProfileId(), profile.getName(), profile.getMaxAgeRating(),
                profile.active());
    }
}
