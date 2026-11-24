package com.cineconecta.catalogo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.cineconecta.common.ApiException;
import com.cineconecta.common.DbErrorTranslator;
import com.cineconecta.common.GlobalExceptionHandler;
import com.cineconecta.security.AuthPrincipal;
import com.cineconecta.security.CurrentUser;
import com.cineconecta.security.JwtService;
import com.cineconecta.security.SecurityConfig;
import com.cineconecta.security.SecurityErrorHandler;

/** Rutas del catálogo con los servicios reales y las bases simuladas: permisos, validación y formato. */
@WebMvcTest
@ContextConfiguration(classes = {CatalogController.class, AdminCatalogController.class})
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtService.class, CurrentUser.class,
        GlobalExceptionHandler.class, DbErrorTranslator.class, CatalogService.class, AdminCatalogService.class})
class CatalogControllersTest {

    private static final String SECRET = UUID.randomUUID() + "" + UUID.randomUUID();
    private static final String ADMIN_URL = "/api/admin/contents";
    private static final String PROFILE = "X-Profile-Id";

    private static final String COMMON = """
            "title": "T", "synopsis": "S", "releaseYear": 2024, "genres": ["drama"], "countries": ["CO"],
            "audioLanguages": ["es"], "subtitleLanguages": [], "ageRating": 12, "coverUrl": "https://img.test/x.jpg"
            """;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRET);
        registry.add("app.jwt.expiration-minutes", () -> "10");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    @MockitoBean
    private ContentRepository contents;

    @MockitoBean
    private ContentStore store;

    @MockitoBean
    private CatalogAccess access;

    private final ObjectId id = new ObjectId();

    private String client() {
        return "Bearer " + jwt.generate("1", AuthPrincipal.CLIENT);
    }

    private String admin() {
        return "Bearer " + jwt.generate("674000000000000000000001", AuthPrincipal.ADMIN);
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post(ADMIN_URL).header(AUTHORIZATION, admin())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Content movie() {
        Content c = new Content();
        c.setId(id);
        c.setType("movie");
        c.setTitle("Noche en Bogotá");
        c.setStatus("published");
        c.setAgeRating(18);
        c.setReleaseYear(2023);
        return c;
    }

    // ------------------------------------------------------------------ permisos

    @Test
    void laAdministracionEsSoloParaAdministradores() throws Exception {
        mvc.perform(get(ADMIN_URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(ADMIN_URL).header(AUTHORIZATION, client())).andExpect(status().isForbidden());
        mvc.perform(patch(ADMIN_URL + "/" + id + "/publish").header(AUTHORIZATION, client()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(store, contents);
    }

    @Test
    void laBusquedaEsSoloParaClientesConPerfil() throws Exception {
        mvc.perform(get("/api/contents").header(PROFILE, 1)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/contents").header(PROFILE, 1).header(AUTHORIZATION, admin()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/contents").header(AUTHORIZATION, client()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
        verifyNoInteractions(store, contents);
    }

    @Test
    void sinPerfilValidoNoSeConsultaElCatalogo() throws Exception {
        when(access.requireMaxAgeRating(1, 3)).thenThrow(
                new ApiException(HttpStatus.FORBIDDEN, "PROFILE_NOT_ALLOWED", "x"));

        for (String path : List.of("/api/contents", "/api/contents/" + id, "/api/contents/" + id + "/seasons")) {
            mvc.perform(get(path).header(PROFILE, 3).header(AUTHORIZATION, client()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PROFILE_NOT_ALLOWED"));
        }
        verifyNoInteractions(store, contents);
    }

    // ------------------------------------------------------------------ cliente

    @Test
    void buscaSoloPublicadosConLaClasificacionDelPerfil() throws Exception {
        when(access.requireMaxAgeRating(1, 1)).thenReturn(16);
        when(store.search(any(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(movie()), PageRequest.of(0, 12), 1));

        mvc.perform(get("/api/contents?q=noche&type=&genre=Acción&language=ES&year=2023")
                        .header(PROFILE, 1).header(AUTHORIZATION, client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toHexString()))
                .andExpect(jsonPath("$.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.total").value(1));

        verify(store).search(new CatalogFilter("noche", null, "published", "acción", "es", 2023), 16,
                new PageRange(0, 12));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=99999999", "size=0", "size=51", "type=otro", "year=abc"})
    void parametrosInvalidosResponden400(String query) throws Exception {
        mvc.perform(get("/api/contents?" + query).header(PROFILE, 1).header(AUTHORIZATION, client()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void unIdMalEscritoResponde400() throws Exception {
        mvc.perform(get("/api/contents/123").header(PROFILE, 1).header(AUTHORIZATION, client()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void elDetalleRespetaLaClasificacion() throws Exception {
        when(access.requireMaxAgeRating(1, 2)).thenReturn(7);
        when(contents.findById(id)).thenReturn(Optional.of(movie()));

        mvc.perform(get("/api/contents/" + id).header(PROFILE, 2).header(AUTHORIZATION, client()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AGE_RATING_NOT_ALLOWED"));
    }

    // ------------------------------------------------------------------ administración

    @Test
    void creaCadaTipo() throws Exception {
        when(contents.insert(any(Content.class))).thenAnswer(call -> {
            Content c = call.getArgument(0);
            c.setId(id);
            return c;
        });

        create("{\"type\":\"movie\"," + COMMON + """
                , "durationSec": 6000, "directors": ["D"], "cast": [{"name": "A", "character": "B"}, {"name": "C"}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toHexString()))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.cast[0].character").value("B"))
                .andExpect(jsonPath("$.seasons").doesNotExist());
        create("{\"type\":\"documentary\"," + COMMON + """
                , "durationSec": 6000, "directors": ["D"], "topics": ["T"], "interviewees": [{"name": "A"}],
                "sources": [{"name": "S", "kind": "archive"}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sources[0].kind").value("archive"));
        create("{\"type\":\"series\"," + COMMON + """
                , "seasons": [{"number": 1, "description": "d", "episodes":
                [{"number": 1, "title": "t", "synopsis": "s", "durationSec": 10}]}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seasons[0].episodes[0].episodeId").isNotEmpty());
    }

    @Test
    void reportaTodosLosCamposInvalidosDeUnaVez() throws Exception {
        create("""
                {"type": "movie", "title": " ", "releaseYear": 1500, "genres": [""], "ageRating": 13,
                 "coverUrl": "javascript:x", "seasons": []}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", allOf(
                        containsString("title:"), containsString("synopsis:"), containsString("releaseYear:"),
                        containsString("genres[0]:"), containsString("countries:"), containsString("ageRating:"),
                        containsString("coverUrl:"), containsString("durationSec:"), containsString("directors:"),
                        containsString("cast:"), containsString("seasons: no corresponde"))));
        verifyNoInteractions(contents);
    }

    @Test
    void elDocumentalExigeTemasYLaSerieNoAdmiteDuracion() throws Exception {
        create("{\"type\":\"documentary\"," + COMMON + ", \"durationSec\": 1, \"directors\": [\"D\"]}")
                .andExpect(jsonPath("$.message").value("topics: debe tener al menos uno en el tipo documentary"));
        create("{\"type\":\"series\"," + COMMON + ", \"durationSec\": 1}")
                .andExpect(jsonPath("$.message").value("durationSec: no corresponde al tipo series"));
        create("{\"type\":\"otro\"," + COMMON + "}")
                .andExpect(jsonPath("$.message").value("type: debe ser movie, series o documentary"));
    }

    @Test
    void numerosRepetidosEnLaMismaPeticionResponden400() throws Exception {
        String episode = "{\"number\": 1, \"title\": \"t\", \"synopsis\": \"s\", \"durationSec\": 10}";
        create("{\"type\":\"series\"," + COMMON + ", \"seasons\": [{\"number\": 1, \"description\": \"d\", \"episodes\": ["
                + episode + "," + episode + "]}, {\"number\": 1, \"description\": \"d\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", allOf(
                        containsString("números de episodio repetidos"),
                        containsString("números de temporada repetidos"))));
    }

    @Test
    void noHayRutaParaBorrar() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(ADMIN_URL + "/" + id).header(AUTHORIZATION, admin()))
                .andExpect(status().isMethodNotAllowed());
    }
}
