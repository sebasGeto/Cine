package com.cineconecta.suscripciones;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Período de suscripción (tabla subscriptions). Solo lectura: las suscripciones se crean
 * únicamente con sp_purchase_subscription (la aplicación no tiene INSERT sobre la tabla).
 */
@Entity
@Table(name = "subscriptions")
@Immutable
@Getter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subscriptionId;

    private Integer accountId;

    private Integer planId;

    /** En UTC. */
    private LocalDateTime startsAt;

    /** startsAt + 30 días (lo exige ck_subs_30_days). */
    private LocalDateTime endsAt;
}
