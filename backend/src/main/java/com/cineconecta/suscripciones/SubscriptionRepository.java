package com.cineconecta.suscripciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Lectura de la tabla subscriptions (la aplicación no puede insertar en ella). */
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByAccountIdOrderByStartsAtDesc(Integer accountId);
}
