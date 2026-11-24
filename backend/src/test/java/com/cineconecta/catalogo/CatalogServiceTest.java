package com.cineconecta.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cineconecta.catalogo.dto.EpisodeResponse;
import com.cineconecta.catalogo.dto.SeasonResponse;
import com.cineconecta.common.ApiException;

/** Reglas de visibilidad y el contrato requirePlayable que usa la reproducción. */
class CatalogServiceTest {

    private final ContentRepository contents = mock(ContentRepository.class);
    private final CatalogService service = new CatalogService(contents, mock(ContentStore.class));

    private final ObjectId id = new ObjectId();
    private final ObjectId e1 = new ObjectId();
    private final ObjectId e2 = new ObjectId();

    private Content movie(int ageRating) {
        Content c = new Content();
        c.setId(id);
        c.setType(ContentType.MOVIE);
        c.setStatus(ContentStatus.PUBLISHED);
        c.setAgeRating(ageRating);
        c.setDurationSec(6600);
        when(contents.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    /** Temporadas y episodios desordenados, como pueden quedar al agregarlos uno por uno. */
    private Content series() {
        Content c = movie(12);
        c.setType(ContentType.SERIES);
        c.setDurationSec(null);
        c.setSeasons(List.of(
                new Season(2, "Segunda", List.of()),
                new Season(1, "Primera", List.of(
                        new Episode(e2, 2, "Dos", "s", 1200),
                        new Episode(e1, 1, "Uno", "s", 1500)))));
        return c;
    }

    private void assertError(Runnable call, int status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus().value()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }

    @Test
    void unaPeliculaSeReproduceSinEpisodio() {
        movie(16);

        assertThat(service.requirePlayable(id, null, 16))
                .isEqualTo(new PlayableItem(id, "movie", null, null, null, 6600));
    }

    @Test
    void unaSerieSeReproducePorEpisodio() {
        series();

        assertThat(service.requirePlayable(id, e2, 18))
                .isEqualTo(new PlayableItem(id, "series", e2, 1, 2, 1200));
        assertError(() -> service.requirePlayable(id, null, 18), 400, "EPISODE_REQUIRED");
        assertError(() -> service.requirePlayable(id, new ObjectId(), 18), 404, "EPISODE_NOT_FOUND");
    }

    @Test
    void laClasificacionDebeEstarPermitida() {
        movie(18);

        assertError(() -> service.requirePlayable(id, null, 7), 403, "AGE_RATING_NOT_ALLOWED");
        assertError(() -> service.detail(id, 7), 403, "AGE_RATING_NOT_ALLOWED");
        assertThat(service.detail(id, 18).id()).isEqualTo(id.toHexString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"draft", "retired"})
    void loNoPublicadoNoExisteParaElCliente(String status) {
        // Clasificación no permitida: aun así debe responder 404 y no revelar que existe
        movie(18).setStatus(status);

        assertError(() -> service.detail(id, 7), 404, "CONTENT_NOT_AVAILABLE");
        assertError(() -> service.requirePlayable(id, null, 18), 404, "CONTENT_NOT_AVAILABLE");
    }

    @Test
    void unIdInexistenteResponde404() {
        assertError(() -> service.detail(new ObjectId(), 18), 404, "CONTENT_NOT_AVAILABLE");
    }

    @Test
    void lasTemporadasYEpisodiosSalenOrdenados() {
        series();

        List<SeasonResponse> seasons = service.seasons(id, 12);
        assertThat(seasons).extracting(SeasonResponse::number).containsExactly(1, 2);
        assertThat(seasons.get(0).episodes()).extracting(EpisodeResponse::number).containsExactly(1, 2);
    }

    @Test
    void soloLasSeriesTienenTemporadas() {
        movie(0);

        assertError(() -> service.seasons(id, 18), 400, "NOT_A_SERIES");
    }
}
