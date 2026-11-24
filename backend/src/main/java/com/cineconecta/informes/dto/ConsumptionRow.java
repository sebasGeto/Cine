package com.cineconecta.informes.dto;

/** Minutos vistos y perfiles distintos de un plan y tipo de contenido en el período. */
public record ConsumptionRow(Integer planId, String planName, String contentType, double minutes, int profiles) {
}
