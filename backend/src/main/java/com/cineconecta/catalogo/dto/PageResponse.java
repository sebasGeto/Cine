package com.cineconecta.catalogo.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Página de resultados; page empieza en 0. */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
