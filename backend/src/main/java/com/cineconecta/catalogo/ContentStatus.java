package com.cineconecta.catalogo;

import java.util.Set;

/** Valores de contents.status. Un contenido nunca se borra: se retira. */
public final class ContentStatus {

    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";
    public static final String RETIRED = "retired";

    public static final Set<String> ALL = Set.of(DRAFT, PUBLISHED, RETIRED);

    private ContentStatus() {
    }
}
