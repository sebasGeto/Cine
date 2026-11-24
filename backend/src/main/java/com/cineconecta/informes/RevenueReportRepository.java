package com.cineconecta.informes;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cineconecta.informes.dto.RevenueRow;

/** Informe de ingresos: vista v_revenue_by_plan, con el usuario de solo lectura. */
@Repository
public class RevenueReportRepository {

    private static final String SQL = """
            SELECT planId, planName, SUM(amount) AS total, COUNT(*) AS subscriptions
            FROM v_revenue_by_plan
            WHERE paidAt >= ? AND paidAt < ?
            GROUP BY planId, planName
            ORDER BY total DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public RevenueReportRepository(
            @Qualifier(ReportDataSourceConfig.REPORT_DATA_SOURCE) DataSource reportDataSource) {
        this.jdbcTemplate = new JdbcTemplate(reportDataSource);
    }

    public List<RevenueRow> revenueByPlan(DateRange range) {
        return jdbcTemplate.query(SQL, (rs, row) -> new RevenueRow(
                rs.getInt("planId"),
                rs.getString("planName"),
                rs.getBigDecimal("total"),
                rs.getLong("subscriptions")), range.start(), range.endExclusive());
    }
}
