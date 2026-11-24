package com.cineconecta.suscripciones.dto;

import java.math.BigDecimal;

import com.cineconecta.suscripciones.Plan;

public record PlanResponse(Integer planId, String name, BigDecimal price, Integer maxProfiles) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getPlanId(), plan.getName(), plan.getPrice(), plan.getMaxProfiles());
    }
}
