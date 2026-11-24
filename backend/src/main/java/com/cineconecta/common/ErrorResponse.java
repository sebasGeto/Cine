package com.cineconecta.common;

/** Formato único de los errores de la API. */
public record ErrorResponse(String code, String message) {
}
