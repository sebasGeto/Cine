package com.cineconecta.suscripciones;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cineconecta.common.ApiException;
import com.cineconecta.suscripciones.dto.PlanResponse;
import com.cineconecta.suscripciones.dto.PurchaseRequest;
import com.cineconecta.suscripciones.dto.PurchaseResponse;

import lombok.RequiredArgsConstructor;

/**
 * Planes y suscripciones. current() y requireCurrent() son el contrato que usan catálogo y
 * reproducción para saber si la cuenta puede ver contenido.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final PlanRepository plans;
    private final SubscriptionDbRepository db;

    /** RF-11. */
    @Transactional(readOnly = true)
    public List<PlanResponse> plans() {
        return plans.findAll(Sort.by("planId")).stream().map(PlanResponse::from).toList();
    }

    /** Suscripción vigente de la cuenta, si tiene. */
    @Transactional(readOnly = true)
    public Optional<CurrentSubscription> current(Integer accountId) {
        return db.findCurrent(accountId);
    }

    /** Suscripción vigente de la cuenta o 403 NO_ACTIVE_SUBSCRIPTION (por ejemplo, si venció). */
    @Transactional(readOnly = true)
    public CurrentSubscription requireCurrent(Integer accountId) {
        return current(accountId).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                "NO_ACTIVE_SUBSCRIPTION", "La cuenta no tiene una suscripción vigente"));
    }

    /**
     * RF-12. No se valida nada en Java: la función de la base decide. Tampoco se captura su
     * excepción aquí: debe salir del método para que la transacción haga rollback, y el
     * GlobalExceptionHandler la traduce (402, 404, 409 o 500).
     */
    @Transactional
    public PurchaseResponse purchase(Integer accountId, PurchaseRequest request) {
        String reference = "PAY-" + UUID.randomUUID();
        Integer subscriptionId = db.purchase(accountId, request.planId(), request.paymentApproved(),
                reference, request.simulateFailure());
        return new PurchaseResponse(subscriptionId);
    }
}
