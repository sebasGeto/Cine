package com.cineconecta.milista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /api/my-list. contentId es el ObjectId en texto (24 hex). */
public record AddToMyListRequest(
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{24}$", message = "contentId no es un identificador válido")
    String contentId
) {}
