package com.cineconecta.informes;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Segunda conexión a PostgreSQL, con el usuario cineconecta_report: solo puede leer la vista
 * v_revenue_by_plan. El usuario de la aplicación (cineconecta_app) no tiene acceso a esa vista.
 *
 * defaultCandidate = false: la conexión principal sigue siendo la de cineconecta_app y esta solo
 * se inyecta donde se pide con @Qualifier.
 */
@Configuration
public class ReportDataSourceConfig {

    static final String REPORT_DATA_SOURCE = "reportDataSource";

    @Bean(name = REPORT_DATA_SOURCE, defaultCandidate = false)
    @Qualifier(REPORT_DATA_SOURCE)
    public DataSource reportDataSource(@Value("${spring.datasource.url}") String url,
                                       @Value("${app.report-datasource.username}") String username,
                                       @Value("${app.report-datasource.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("informes");
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setReadOnly(true);
        ds.setMaximumPoolSize(2);
        ds.setMinimumIdle(0);
        ds.setConnectionTimeout(3000);
        // El pool se crea con la primera consulta; si PostgreSQL no está, el error es de conexión (503)
        ds.setInitializationFailTimeout(-1);
        return ds;
    }
}
