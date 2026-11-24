package com.cineconecta.suscripciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.common.ApiException;
import com.cineconecta.security.CurrentUser;
import com.cineconecta.suscripciones.dto.PurchaseRequest;
import com.cineconecta.suscripciones.dto.PurchaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Suscripción de la cuenta autenticada. Solo CLIENT; la cuenta sale del token. */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUser currentUser;

    /** RF-12. 201 {subscriptionId}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse purchase(@Valid @RequestBody PurchaseRequest request) {
        return subscriptionService.purchase(currentUser.accountId(), request);
    }

    /** RF-13. 404 si no hay suscripción vigente. */
    @GetMapping("/current")
    public CurrentSubscription current() {
        return subscriptionService.current(currentUser.accountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_ACTIVE_SUBSCRIPTION",
                        "La cuenta no tiene una suscripción vigente"));
    }
}
