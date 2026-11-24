package com.cineconecta.cuentas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una suscripción de la cuenta con su pago. Fechas en UTC. */
public record AccountHistoryItem(
        Integer subscriptionId,
        String planName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean isCurrent,
        Integer paymentId,
        LocalDateTime paidAt,
        BigDecimal amount,
        String reference) {
}
