package com.cineconecta.informes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;

import com.cineconecta.common.ApiException;

/**
 * Rango de fechas de un informe, en UTC. Ambos días se incluyen completos: el filtro es
 * desde las 00:00 de "from" y antes de las 00:00 del día siguiente a "to".
 */
public record DateRange(LocalDate from, LocalDate to) {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    public DateRange {
        if (from.getYear() < MIN_YEAR || to.getYear() > MAX_YEAR || from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "La fecha inicial no puede ser posterior a la final (años " + MIN_YEAR + " a " + MAX_YEAR + ")");
        }
    }

    public LocalDateTime start() {
        return from.atStartOfDay();
    }

    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    public Instant startInstant() {
        return start().toInstant(ZoneOffset.UTC);
    }

    public Instant endExclusiveInstant() {
        return endExclusive().toInstant(ZoneOffset.UTC);
    }
}
