package com.cineconecta.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.cineconecta.catalogo.dto.AdminContentResponse;
import com.cineconecta.catalogo.dto.CastMemberDto;
import com.cineconecta.catalogo.dto.ContentRequest;
import com.cineconecta.catalogo.dto.EpisodeRequest;
import com.cineconecta.catalogo.dto.EpisodeUpdateRequest;
import com.cineconecta.catalogo.dto.SeasonRequest;
import com.cineconecta.catalogo.dto.SeasonUpdateRequest;
import com.cineconecta.common.ApiException;

/** Qué responde la administración cuando una escritura condicional no tiene efecto. */
class AdminCatalogServiceTest {

    private static final EpisodeRequest EPISODE = new EpisodeRequest(1, "Piloto", "s", 1500);

    private final ContentRepository contents = mock(ContentRepository.class);
    private final ContentStore store = mock(ContentStore.class);
    private final AdminCatalogService service = new AdminCatalogService(contents, store);
    private final ObjectId id = new ObjectId();

    private static ContentRequest request(String type, List<CastMemberDto> cast, List<SeasonRequest> seasons) {
        boolean movie = "movie".equals(type);
        return new ContentRequest(type, " Título ", "s", 2024, List.of("Drama", "drama"), List.of("CO"),
                List.of("ES"), List.of(), 12, "https://img.test/x.jpg", movie ? 6000 : null,
                movie ? List.of("Directora") : null, cast, null, null, null, seasons);
    }

    private static ContentRequest series(SeasonRequest... seasons) {
        return request("series", null, List.of(seasons));
    }

    private void stored(String type) {
        Content c = new Content();
        c.setId(id);
        c.setType(type);
        c.setStatus(ContentStatus.DRAFT);
        if (c.isSeries()) {
            c.setSeasons(List.of(new Season(1, "Primera", List.of())));
        }
        when(contents.findById(id)).thenReturn(Optional.of(c));
    }

    private void insertWorks() {
        when(contents.insert(any(Content.class))).thenAnswer(call -> {
            Content c = call.getArgument(0);
            c.setId(id);
            return c;
        });
    }

    private void assertError(Runnable call, int status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus().value()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }

    @Test
    void creaEnBorradorYNormalizaLosTextos() {
        insertWorks();

        AdminContentResponse created = service.create(
                request("movie", List.of(new CastMemberDto("Ana", " ")), null));

        assertThat(created.status()).isEqualTo("draft");
        assertThat(created.schemaVersion()).isEqualTo(1);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.content().title()).isEqualTo("Título");
        assertThat(created.content().genres()).containsExactly("drama");
        assertThat(created.content().audioLanguages()).containsExactly("es");
        assertThat(created.content().cast()).containsExactly(new CastMemberDto("Ana", null));
    }

    @Test
    void cadaEpisodioRecibeSuPropioId() {
        insertWorks();

        var episodes = service.create(series(new SeasonRequest(1, "d",
                List.of(EPISODE, new EpisodeRequest(2, "Dos", "s", 1500))))).content().seasons().get(0).episodes();

        assertThat(episodes).extracting("episodeId").doesNotHaveDuplicates().doesNotContainNull();
    }

    @Test
    void unaSerieSeCreaConAlMenosUnaTemporada() {
        assertError(() -> service.create(series()), 400, "VALIDATION_ERROR");
        assertError(() -> service.create(request("series", null, null)), 400, "VALIDATION_ERROR");
    }

    @Test
    void elChoqueDeTemporadasVaciasSeExplica() {
        when(contents.insert(any(Content.class))).thenThrow(new DuplicateKeyException("E11000"));

        assertError(() -> service.create(series(new SeasonRequest(1, "d", null))), 409, "EMPTY_SEASON_CONFLICT");
        // Con episodios el duplicado sería otro: lo traduce DbErrorTranslator
        assertThatThrownBy(() -> service.create(series(new SeasonRequest(1, "d", List.of(EPISODE)))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void elTipoNoSePuedeCambiar() {
        stored("series");
        when(store.updateFields(any(), anyString(), anyMap(), any())).thenReturn(false);

        assertError(() -> service.update(id, request("movie", List.of(), null)), 400, "TYPE_CANNOT_CHANGE");
    }

    @Test
    void unaActualizacionNuncaTocaIdTipoEstadoNiTemporadas() {
        var fields = ContentMapper.updatedFields(series(new SeasonRequest(1, "d", List.of(EPISODE))));

        assertThat(fields).containsKeys("title", "genres", "ageRating")
                .doesNotContainKeys("_id", "id", "type", "status", "createdAt", "seasons", "durationSec");
    }

    @Test
    void numerosRepetidosResponden409() {
        stored("series");

        assertError(() -> service.addSeason(id, new SeasonRequest(1, "d", null)), 409, "SEASON_NUMBER_EXISTS");
        assertError(() -> service.addEpisode(id, 1, EPISODE), 409, "EPISODE_NUMBER_EXISTS");
    }

    @Test
    void temporadaOEpisodioInexistentesResponden404() {
        stored("series");

        assertError(() -> service.updateSeason(id, 9, new SeasonUpdateRequest("d")), 404, "SEASON_NOT_FOUND");
        assertError(() -> service.addEpisode(id, 9, EPISODE), 404, "SEASON_NOT_FOUND");
        when(store.updateEpisode(any(), anyInt(), anyInt(), any(), any(), anyInt(), any())).thenReturn(false);
        var changes = new EpisodeUpdateRequest("t", "s", 100);
        assertError(() -> service.updateEpisode(id, 1, 9, changes), 404, "EPISODE_NOT_FOUND");
        assertError(() -> service.updateEpisode(id, 9, 1, changes), 404, "SEASON_NOT_FOUND");
    }

    @Test
    void soloLasSeriesAdmitenTemporadas() {
        stored("movie");

        assertError(() -> service.addSeason(id, new SeasonRequest(1, "d", null)), 400, "NOT_A_SERIES");
        assertError(() -> service.addEpisode(id, 1, EPISODE), 400, "NOT_A_SERIES");
    }

    @Test
    void unContenidoInexistenteResponde404() {
        when(contents.findById(eq(id))).thenReturn(Optional.empty());

        assertError(() -> service.get(id), 404, "CONTENT_NOT_FOUND");
        assertError(() -> service.publish(id), 404, "CONTENT_NOT_FOUND");
        assertError(() -> service.retire(id), 404, "CONTENT_NOT_FOUND");
        assertError(() -> service.update(id, request("movie", List.of(), null)), 404, "CONTENT_NOT_FOUND");
    }
}
