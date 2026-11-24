package com.cineconecta.catalogo;

import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Filtros de búsqueda; null = sin filtro. Los textos vacíos cuentan como null (así llegan los
 * campos de formulario sin llenar) y género e idioma se comparan como se guardan, en minúscula.
 */
record CatalogFilter(String title, String type, String status, String genre, String language, Integer year) {

    CatalogFilter {
        title = blankToNull(title);
        type = oneOf("type", blankToNull(type), ContentType.ALL);
        status = oneOf("status", blankToNull(status), ContentStatus.ALL);
        genre = Texts.lower(blankToNull(genre));
        language = Texts.lower(blankToNull(language));
    }

    /** Búsqueda del cliente: solo publicados. */
    static CatalogFilter published(String title, String type, String genre, String language, Integer year) {
        return new CatalogFilter(title, type, ContentStatus.PUBLISHED, genre, language, year);
    }

    /** Listado del administrador: cualquier estado. */
    static CatalogFilter admin(String title, String type, String status) {
        return new CatalogFilter(title, type, status, null, null, null);
    }

    /** Se buscó un título sin ninguna palabra ("¿?"): $text no admite una expresión vacía. */
    boolean matchesNothing() {
        return title != null && Texts.allWords(title).isEmpty();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String oneOf(String name, String value, Set<String> allowed) {
        if (value != null && !allowed.contains(value)) {
            throw CatalogErrors.invalidParameter(name);
        }
        return value;
    }
}
