package com.cineconecta.suscripciones;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Las dos piezas de lógica de suscripciones que viven en PostgreSQL: la vista
 * v_current_subscriptions y la función sp_purchase_subscription. Aquí no se repite ninguna regla.
 */
@Repository
@RequiredArgsConstructor
public class SubscriptionDbRepository {

    private static final String CURRENT_SQL = """
            SELECT subscriptionId, planId, planName, maxProfiles, endsAt
            FROM v_current_subscriptions
            WHERE accountId = ?
            """;

    private static final String PURCHASE_SQL = "SELECT sp_purchase_subscription(?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    /** Vigente = startsAt <= ahora (UTC) < endsAt; la restricción de traslapes garantiza a lo sumo una. */
    public Optional<CurrentSubscription> findCurrent(Integer accountId) {
        return jdbcTemplate.query(CURRENT_SQL, (rs, row) -> new CurrentSubscription(
                rs.getInt("subscriptionId"),
                rs.getInt("planId"),
                rs.getString("planName"),
                rs.getInt("maxProfiles"),
                rs.getObject("endsAt", LocalDateTime.class)), accountId)
                .stream().findFirst();
    }

    /**
     * RF-12. La función bloquea la cuenta, valida pago, vigencia y cupo, e inserta suscripción y
     * pago en la misma transacción. Si falla lanza una excepción con el código al inicio del
     * mensaje (PAYMENT_REJECTED, SUBSCRIPTION_ALREADY_ACTIVE...), que traduce DbErrorTranslator.
     */
    public Integer purchase(Integer accountId, Integer planId, boolean paymentApproved,
                            String reference, boolean simulateFailure) {
        return jdbcTemplate.queryForObject(PURCHASE_SQL, Integer.class,
                accountId, planId, paymentApproved, reference, simulateFailure);
    }
}
