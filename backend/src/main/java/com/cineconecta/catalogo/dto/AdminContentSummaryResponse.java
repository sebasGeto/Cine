package com.cineconecta.catalogo.dto;

import java.time.Instant;

import com.cineconecta.catalogo.Content;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Fila del listado del administrador (RF-20). */
@JsonInclude(Include.NON_NULL)
public record AdminContentSummaryResponse(
        @JsonUnwrapped ContentSummaryResponse content,
        String status,
        Instant publishedAt,
        Instant retiredAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminContentSummaryResponse from(Content c) {
        return new AdminContentSummaryResponse(ContentSummaryResponse.from(c), c.getStatus(),
                c.getPublishedAt(), c.getRetiredAt(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
