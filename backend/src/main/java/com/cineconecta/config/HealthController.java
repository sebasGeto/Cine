package com.cineconecta.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifica que la aplicación se conecta a las dos bases de datos.
 * GET /api/health → { "postgres": "ok" | "error", "mongo": "ok" | "error" }
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, MongoTemplate mongoTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public Map<String, String> health() {
        Map<String, String> status = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("postgres", "ok");
        } catch (Exception e) {
            status.put("postgres", "error");
        }
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
            status.put("mongo", "ok");
        } catch (Exception e) {
            status.put("mongo", "error");
        }
        return status;
    }
}
