package com.cineconecta.informes;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.informes.dto.ConsumptionRow;
import com.cineconecta.informes.dto.RevenueRow;

import lombok.RequiredArgsConstructor;

/** Informes para la administración. Solo ADMIN (por la ruta /api/admin). */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** RF-35. Fechas AAAA-MM-DD en UTC, ambas incluidas. */
    @GetMapping("/revenue")
    public List<RevenueRow> revenue(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                    @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return reportService.revenue(new DateRange(from, to));
    }

    /** RF-36. Fechas AAAA-MM-DD en UTC, ambas incluidas. */
    @GetMapping("/consumption")
    public List<ConsumptionRow> consumption(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return reportService.consumption(new DateRange(from, to));
    }
}
