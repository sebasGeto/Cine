package com.cineconecta.security;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.common.DbErrorTranslator;
import com.cineconecta.common.GlobalExceptionHandler;

/** Reglas de acceso por ruta y rol, con rutas de mentira y sin bases de datos. */
@WebMvcTest
@ContextConfiguration(classes = SecurityRulesTest.Routes.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtService.class, CurrentUser.class,
        GlobalExceptionHandler.class, DbErrorTranslator.class, SecurityRulesTest.Routes.class})
class SecurityRulesTest {

    private static final String SECRET = UUID.randomUUID() + "" + UUID.randomUUID();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRET);
        registry.add("app.jwt.expiration-minutes", () -> "10");
    }

    @RestController
    static class Routes {
        private final CurrentUser currentUser;

        Routes(CurrentUser currentUser) {
            this.currentUser = currentUser;
        }

        @GetMapping("/api/health")
        String health() {
            return "ok";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "ok";
        }

        @GetMapping("/api/plans")
        String plans() {
            return "ok";
        }

        @PostMapping("/api/plans")
        String createPlan() {
            return "ok";
        }

        @GetMapping("/api/admin/reports/revenue")
        String report() {
            return currentUser.adminId();
        }

        @GetMapping("/api/account/history")
        String history() {
            return String.valueOf(currentUser.accountId());
        }

        @GetMapping("/api/profiles")
        String broken() {
            throw new IllegalStateException("detalle interno");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwt;

    private String client() {
        return "Bearer " + jwt.generate("7", AuthPrincipal.CLIENT);
    }

    private String admin() {
        return "Bearer " + jwt.generate("674000000000000000000001", AuthPrincipal.ADMIN);
    }

    @Test
    void lasRutasPublicasNoPidenToken() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/api/plans")).andExpect(status().isOk());
    }

    @Test
    void soloLaLecturaDePlanesEsPublica() throws Exception {
        mvc.perform(post("/api/plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenResponde401EnJson() throws Exception {
        mvc.perform(get("/api/account/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/admin/reports/revenue")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoOVencidoResponde401() throws Exception {
        String expired = new JwtService(SECRET, -1).generate("7", AuthPrincipal.CLIENT);
        mvc.perform(get("/api/account/history").header(AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/history").header(AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        // El login sigue funcionando aunque el navegador envíe el token vencido
        mvc.perform(post("/api/auth/login").header(AUTHORIZATION, "Bearer " + expired)).andExpect(status().isOk());
    }

    @Test
    void elClienteNoEntraAAdministracion() throws Exception {
        mvc.perform(get("/api/admin/reports/revenue").header(AUTHORIZATION, client()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void elAdministradorNoEntraARutasDeCliente() throws Exception {
        mvc.perform(get("/api/account/history").header(AUTHORIZATION, admin())).andExpect(status().isForbidden());
    }

    @Test
    void cadaRolEntraALoSuyoConElIdDelToken() throws Exception {
        mvc.perform(get("/api/account/history").header(AUTHORIZATION, client()))
                .andExpect(status().isOk()).andExpect(content().string("7"));
        mvc.perform(get("/api/admin/reports/revenue").header(AUTHORIZATION, admin()))
                .andExpect(status().isOk()).andExpect(content().string("674000000000000000000001"));
    }

    @Test
    void unErrorInesperadoNoExponeDetalles() throws Exception {
        mvc.perform(get("/api/profiles").header(AUTHORIZATION, client()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno"));
    }

    @Test
    void unaRutaInexistenteResponde404ConElMismoFormato() throws Exception {
        mvc.perform(get("/api/no-existe").header(AUTHORIZATION, client()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
