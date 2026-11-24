package com.cineconecta.catalogo;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Limpieza de los textos que entran al catálogo. */
final class Texts {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    private Texts() {
    }

    static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /** null si el texto está vacío: MongoDB no guarda el campo. */
    static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Sin espacios sobrantes ni repetidos, en el mismo orden. */
    static List<String> clean(List<String> values) {
        return values == null ? null : values.stream().map(String::trim).distinct().toList();
    }

    /** Como clean, en minúscula: "Drama" y "drama" son el mismo género. */
    static List<String> cleanLower(List<String> values) {
        return values == null ? null : values.stream().map(String::trim).map(Texts::lower).distinct().toList();
    }

    /**
     * Expresión de $text que exige TODAS las palabras: último faro se busca como "último" "faro".
     * Sin comillas MongoDB devolvería los títulos con cualquiera de ellas. Solo pasan letras y
     * números, así que los operadores de $text (- para excluir, comillas) no tienen efecto.
     * Devuelve "" si no hay ninguna palabra.
     */
    static String allWords(String query) {
        return WORD.matcher(query).results()
                .map(m -> '"' + m.group() + '"')
                .collect(Collectors.joining(" "));
    }
}
