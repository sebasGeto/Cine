package com.cineconecta.catalogo.dto;

import java.time.Instant;

import com.cineconecta.catalogo.Content;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Contenido completo para el administrador: el detalle más los datos de gestión. */
@JsonInclude(Include.NON_NULL)
public record AdminContentResponse(
        @JsonUnwrapped ContentDetailResponse content,
        String status,
        Instant publishedAt,
        Instant retiredAt,
        Integer schemaVersion,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminContentResponse from(Content c) {
        return new AdminContentResponse(ContentDetailResponse.from(c), c.getStatus(), c.getPublishedAt(),
                c.getRetiredAt(), c.getSchemaVersion(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
