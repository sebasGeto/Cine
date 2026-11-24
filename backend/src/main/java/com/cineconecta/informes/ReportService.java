package com.cineconecta.informes;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cineconecta.informes.dto.RevenueRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RevenueReportRepository revenueReport;

    /** RF-35. Solo usa PostgreSQL. */
    public List<RevenueRow> revenue(DateRange range) {
        return revenueReport.revenueByPlan(range);
    }
}
