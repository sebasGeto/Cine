package com.cineconecta.suscripciones;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.suscripciones.dto.PlanResponse;

import lombok.RequiredArgsConstructor;

/** Planes disponibles. GET es público (SecurityConfig). */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final SubscriptionService subscriptionService;

    /** RF-11. */
    @GetMapping
    public List<PlanResponse> list() {
        return subscriptionService.plans();
    }
}
