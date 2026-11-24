package com.cineconecta.suscripciones;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cineconecta.common.DbErrorTranslator;
import com.cineconecta.common.GlobalExceptionHandler;
import com.cineconecta.security.AuthPrincipal;
import com.cineconecta.security.CurrentUser;
import com.cineconecta.security.JwtService;
import com.cineconecta.security.SecurityConfig;
import com.cineconecta.security.SecurityErrorHandler;

/**
 * Rutas de planes y suscripciones con el servicio real y la base simulada: comprueba que la
 * cuenta sale del token y que los errores de sp_purchase_subscription llegan con su código HTTP.
 */
@WebMvcTest
@ContextConfiguration(classes = {PlanController.class, SubscriptionController.class})
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtService.class, CurrentUser.class,
        GlobalExceptionHandler.class, DbErrorTranslator.class, SubscriptionService.class})
class SubscriptionControllerTest {

    private static final String SECRET = UUID.randomUUID() + "" + UUID.randomUUID();

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
    private PlanRepository plans;

    @MockitoBean
    private SubscriptionDbRepository db;

    private String carla() {
        return "Bearer " + jwt.generate("3", AuthPrincipal.CLIENT);
    }

    /** Error tal como lo lanza la función: SQLState P0001 y "CODIGO: texto". */
    private static UncategorizedSQLException dbError(String message) {
        ServerErrorMessage server = new ServerErrorMessage("SERROR\0C" + "P0001" + "\0M" + message + "\0");
        SQLException cause = new PSQLException(server);
        return new UncategorizedSQLException("sp_purchase_subscription", "SELECT ...", cause);
    }

    private void purchaseFails(String message) {
        when(db.purchase(eq(3), eq(2), Mockito.anyBoolean(), any(), Mockito.anyBoolean()))
                .thenThrow(dbError(message));
    }

    private org.springframework.test.web.servlet.ResultActions purchase(String body) throws Exception {
        return mvc.perform(post("/api/subscriptions").header(AUTHORIZATION, carla())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void losPlanesSonPublicos() throws Exception {
        Plan plan = Mockito.mock(Plan.class);
        when(plan.getPlanId()).thenReturn(1);
        when(plan.getName()).thenReturn("Básico");
        when(plan.getPrice()).thenReturn(new BigDecimal("16900.00"));
        when(plan.getMaxProfiles()).thenReturn(1);
        when(plans.findAll(any(Sort.class))).thenReturn(java.util.List.of(plan));

        mvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planId").value(1))
                .andExpect(jsonPath("$[0].name").value("Básico"))
                .andExpect(jsonPath("$[0].price").value(16900.00))
                .andExpect(jsonPath("$[0].maxProfiles").value(1));
    }

    @Test
    void contratarUsaLaCuentaDelTokenYUnaReferenciaNueva() throws Exception {
        when(db.purchase(eq(3), eq(2), eq(true), startsWith("PAY-"), eq(false))).thenReturn(12);

        purchase("{\"planId\":2,\"paymentApproved\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(12));
    }

    @Test
    void contratarSinTokenResponde401() throws Exception {
        mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":2,\"paymentApproved\":true}"))
                .andExpect(status().isUnauthorized());
        verify(db, never()).purchase(any(), any(), Mockito.anyBoolean(), any(), Mockito.anyBoolean());
    }

    @Test
    void faltanDatosResponde400() throws Exception {
        purchase("{\"planId\":2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void pagoRechazadoResponde402() throws Exception {
        purchaseFails("PAYMENT_REJECTED: el pago fue rechazado");
        purchase("{\"planId\":2,\"paymentApproved\":false}")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED"));
    }

    @Test
    void suscripcionVigenteResponde409() throws Exception {
        purchaseFails("SUBSCRIPTION_ALREADY_ACTIVE: la cuenta 3 ya tiene una suscripción vigente");
        purchase("{\"planId\":2,\"paymentApproved\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_ALREADY_ACTIVE"));
    }

    @Test
    void falloSimuladoResponde500() throws Exception {
        purchaseFails("SIMULATED_FAILURE: fallo simulado antes de registrar el pago");
        purchase("{\"planId\":2,\"paymentApproved\":true,\"simulateFailure\":true}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SIMULATED_FAILURE"));
    }

    @Test
    void suscripcionActualOResponde404() throws Exception {
        LocalDateTime endsAt = LocalDateTime.of(2026, 12, 20, 10, 0);
        when(db.findCurrent(3)).thenReturn(Optional.of(new CurrentSubscription(12, 2, "Estándar", 2, endsAt)));
        mvc.perform(get("/api/subscriptions/current").header(AUTHORIZATION, carla()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Estándar"))
                .andExpect(jsonPath("$.maxProfiles").value(2));

        when(db.findCurrent(3)).thenReturn(Optional.empty());
        mvc.perform(get("/api/subscriptions/current").header(AUTHORIZATION, carla()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_SUBSCRIPTION"));
    }
}
