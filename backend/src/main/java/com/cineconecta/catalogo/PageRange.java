package com.cineconecta.catalogo;

import org.springframework.data.domain.PageRequest;

/** Página pedida por el cliente: page desde 0 y size de 1 a 50. */
record PageRange(int page, int size) {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_SIZE = 50;

    PageRange {
        if (page < 0 || page > MAX_PAGE) {
            throw CatalogErrors.invalidParameter("page");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw CatalogErrors.invalidParameter("size");
        }
    }

    PageRequest toPageRequest() {
        return PageRequest.of(page, size);
    }
}
