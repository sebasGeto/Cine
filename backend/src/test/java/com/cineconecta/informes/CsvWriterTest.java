package com.cineconecta.informes;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvWriterTest {

    private static String csv(Object... cells) {
        return new String(CsvWriter.write(List.of("a", "b"), List.of(List.of(cells))), StandardCharsets.UTF_8);
    }

    @Test
    void escribeBomEncabezadoYTildes() {
        assertThat(csv("Básico", 77.5)).isEqualTo("﻿a,b\r\nBásico,77.5\r\n");
    }

    @Test
    void escapaComasYComillas() {
        assertThat(csv("uno, \"dos\"", 1)).contains("\"uno, \"\"dos\"\"\",1");
    }

    @Test
    void neutralizaFormulasPeroNoNumerosNegativos() {
        assertThat(csv("=SUMA(A1)", -5)).contains("'=SUMA(A1),-5");
    }
}
