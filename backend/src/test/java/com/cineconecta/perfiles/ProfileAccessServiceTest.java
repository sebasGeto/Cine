package com.cineconecta.perfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.cineconecta.common.ApiException;

class ProfileAccessServiceTest {

    private final ProfileRepository repository = mock(ProfileRepository.class);
    private final ProfileAccessService service = new ProfileAccessService(repository);

    private static Profile profile(int id, int accountId, String active) {
        Profile p = new Profile();
        p.setProfileId(id);
        p.setAccountId(accountId);
        p.setName("Tomás");
        p.setMaxAgeRating(7);
        p.setIsActive(active);
        return p;
    }

    @Test
    void devuelveElPerfilActivoDeLaCuenta() {
        when(repository.findByProfileIdAndAccountId(2, 1)).thenReturn(Optional.of(profile(2, 1, "Y")));

        assertThat(service.verify(1, 2)).isEqualTo(new ActiveProfile(2, 1, "Tomás", 7));
    }

    @Test
    void perfilInactivoResponde403() {
        when(repository.findByProfileIdAndAccountId(7, 4)).thenReturn(Optional.of(profile(7, 4, "N")));

        assertNotAllowed(4, 7);
    }

    @Test
    void perfilDeOtraCuentaOInexistenteResponde403() {
        when(repository.findByProfileIdAndAccountId(2, 4)).thenReturn(Optional.empty());

        assertNotAllowed(4, 2);
        assertNotAllowed(4, null);
    }

    private void assertNotAllowed(Integer accountId, Integer profileId) {
        assertThatThrownBy(() -> service.verify(accountId, profileId))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCode()).isEqualTo("PROFILE_NOT_ALLOWED");
                });
    }
}
