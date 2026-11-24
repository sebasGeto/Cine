package com.cineconecta.informes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cineconecta.informes.ConsumptionReportRepository.Consumption;
import com.cineconecta.informes.dto.ConsumptionRow;

class ReportServiceTest {

    private final ConsumptionReportRepository mongo = mock(ConsumptionReportRepository.class);
    private final PlanNameRepository postgres = mock(PlanNameRepository.class);
    private final ReportService service = new ReportService(mock(RevenueReportRepository.class), mongo, postgres);

    @Test
    void completaElNombreDelPlanYConservaLasReferenciasRotas() {
        DateRange range = new DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        when(mongo.consumptionByPlanAndType(range)).thenReturn(List.of(
                new Consumption(1, "movie", 305.0, 3), new Consumption(99, "series", 10.0, 1)));
        when(postgres.findAll()).thenReturn(Map.of(1, "Básico"));

        assertThat(service.consumption(range)).containsExactly(
                new ConsumptionRow(1, "Básico", "movie", 305.0, 3),
                new ConsumptionRow(99, "Plan 99 (sin referencia)", "series", 10.0, 1));
    }
}
