package com.cineconecta.catalogo;

import java.util.Set;

/** Valores de contents.type. */
public final class ContentType {

    public static final String MOVIE = "movie";
    public static final String SERIES = "series";
    public static final String DOCUMENTARY = "documentary";

    public static final Set<String> ALL = Set.of(MOVIE, SERIES, DOCUMENTARY);

    private ContentType() {
    }
}
