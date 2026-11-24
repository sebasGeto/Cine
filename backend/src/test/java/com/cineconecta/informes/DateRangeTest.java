package com.cineconecta.informes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.cineconecta.common.ApiException;

class DateRangeTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    @Test
    void unSoloDiaSeIncluyeCompleto() {
        DateRange range = new DateRange(DAY, DAY);

        assertThat(range.start()).isEqualTo(LocalDateTime.of(2026, 11, 24, 0, 0));
        assertThat(range.endExclusive()).isEqualTo(LocalDateTime.of(2026, 11, 25, 0, 0));
        assertThat(range.startInstant()).hasToString("2026-11-24T00:00:00Z");
    }

    @Test
    void rechazaRangosInvertidosOAbsurdos() {
        assertThatThrownBy(() -> new DateRange(DAY, DAY.minusDays(1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new DateRange(DAY, LocalDate.MAX)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new DateRange(LocalDate.MIN, DAY)).isInstanceOf(ApiException.class);
    }
}
