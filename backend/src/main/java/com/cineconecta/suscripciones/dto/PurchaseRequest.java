package com.cineconecta.suscripciones.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Contratación con pago simulado. simulateFailure es solo para la demo de atomicidad
 * (si no se envía, es false).
 */
public record PurchaseRequest(
        @NotNull(message = "es obligatorio")
        Integer planId,

        @NotNull(message = "es obligatorio")
        Boolean paymentApproved,

        Boolean simulateFailure) {

    public PurchaseRequest {
        simulateFailure = Boolean.TRUE.equals(simulateFailure);
    }
}
