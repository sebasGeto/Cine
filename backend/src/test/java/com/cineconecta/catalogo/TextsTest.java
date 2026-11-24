package com.cineconecta.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "faro | \"faro\"",
            "último  faro | \"último\" \"faro\"",
            "-faro | \"faro\"",
            "\"el faro\" | \"el\" \"faro\"",
            "Selva viva: el Amazonas | \"Selva\" \"viva\" \"el\" \"Amazonas\""
    })
    void laBusquedaExigeTodasLasPalabrasYNoAdmiteOperadores(String query, String expected) {
        assertThat(Texts.allWords(query)).isEqualTo(expected);
    }

    @Test
    void sinPalabrasNoHayExpresion() {
        assertThat(Texts.allWords("¿?!")).isEmpty();
    }

    @Test
    void quitaEspaciosYRepetidos() {
        assertThat(Texts.cleanLower(List.of(" Drama", "drama ", "Acción"))).containsExactly("drama", "acción");
        assertThat(Texts.clean(List.of("CO ", "CO", "MX"))).containsExactly("CO", "MX");
    }
}
