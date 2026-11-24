package com.cineconecta.informes;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Parte de PostgreSQL del informe de consumo: nombres de los planes. */
@Repository
@RequiredArgsConstructor
public class PlanNameRepository {

    private final JdbcTemplate jdbcTemplate;

    public Map<Integer, String> findAll() {
        Map<Integer, String> names = new HashMap<>();
        jdbcTemplate.query("SELECT planId, name FROM plans",
                rs -> { names.put(rs.getInt("planId"), rs.getString("name")); });
        return names;
    }
}
