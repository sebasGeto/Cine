package com.cineconecta.suscripciones;

import java.time.LocalDateTime;

/** Suscripción vigente de una cuenta (fila de v_current_subscriptions). endsAt en UTC. */
public record CurrentSubscription(
        Integer subscriptionId,
        Integer planId,
        String planName,
        Integer maxProfiles,
        LocalDateTime endsAt) {
}
