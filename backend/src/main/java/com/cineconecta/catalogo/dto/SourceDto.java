package com.cineconecta.catalogo.dto;

import com.cineconecta.catalogo.Source;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonInclude(Include.NON_NULL)
public record SourceDto(
        @NotBlank(message = "es obligatorio") String name,
        @NotNull(message = "es obligatorio")
        @Pattern(regexp = "institution|archive|publication", message = "debe ser institution, archive o publication")
        String kind,
        @Pattern(regexp = "|https?://\\S+", message = "debe empezar por http:// o https://") String url) {

    static SourceDto from(Source s) {
        return new SourceDto(s.getName(), s.getKind(), s.getUrl());
    }
}
