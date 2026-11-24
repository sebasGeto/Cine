package com.cineconecta.informes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Escribe CSV en UTF-8 (RFC 4180). Empieza con BOM para que Excel muestre bien las tildes.
 */
final class CsvWriter {

    private static final String BOM = "﻿";
    private static final String LINE_END = "\r\n";

    private CsvWriter() {
    }

    static byte[] write(List<String> header, List<List<Object>> rows) {
        StringBuilder csv = new StringBuilder(BOM);
        appendLine(csv, header);
        rows.forEach(row -> appendLine(csv, row));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendLine(StringBuilder csv, List<?> values) {
        csv.append(values.stream().map(CsvWriter::cell).collect(Collectors.joining(","))).append(LINE_END);
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        // Un texto que empieza así lo ejecutaría Excel como fórmula
        if (value instanceof String && !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean quote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        return quote ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
