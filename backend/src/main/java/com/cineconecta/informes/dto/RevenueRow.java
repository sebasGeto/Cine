package com.cineconecta.informes.dto;

import java.math.BigDecimal;

/** Total recaudado (COP) y suscripciones adquiridas de un plan en el período. */
public record RevenueRow(Integer planId, String planName, BigDecimal total, long subscriptions) {
}
