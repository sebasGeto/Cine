package com.cineconecta.informes;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cineconecta.informes.ConsumptionReportRepository.Consumption;
import com.cineconecta.informes.dto.ConsumptionRow;
import com.cineconecta.informes.dto.RevenueRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final RevenueReportRepository revenueReport;
    private final ConsumptionReportRepository consumptionReport;
    private final PlanNameRepository planNames;

    /** RF-35. Solo usa PostgreSQL. */
    public List<RevenueRow> revenue(DateRange range) {
        return revenueReport.revenueByPlan(range);
    }

    /**
     * RF-36. Combina las dos bases: MongoDB agrupa las sesiones por planId y PostgreSQL da el nombre
     * de cada plan. Si alguna no está disponible se responde 503: son solo lecturas, no hay nada
     * que deshacer.
     */
    public List<ConsumptionRow> consumption(DateRange range) {
        List<Consumption> consumption = consumptionReport.consumptionByPlanAndType(range);
        Map<Integer, String> names = planNames.findAll();
        return consumption.stream()
                .map(c -> new ConsumptionRow(c.planId(), planName(names, c.planId()), c.contentType(),
                        c.minutes(), c.profiles()))
                .toList();
    }

    /** No hay clave foránea entre las bases: una referencia rota se muestra y se registra, no se oculta. */
    private static String planName(Map<Integer, String> names, Integer planId) {
        String name = names.get(planId);
        if (name == null) {
            log.warn("viewingSessions referencia el plan {}, que no existe en PostgreSQL", planId);
            return "Plan " + planId + " (sin referencia)";
        }
        return name;
    }
}
