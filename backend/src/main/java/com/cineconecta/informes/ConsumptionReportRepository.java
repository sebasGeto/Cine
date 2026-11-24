package com.cineconecta.informes;

import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Parte de MongoDB del informe de consumo (consulta 6.5 de la entrega previa).
 * Cada sesión guarda el plan vigente cuando se inició, así que no hay que buscarlo de nuevo.
 * El $match inicial aprovecha el índice ix_sessions_report.
 */
@Repository
@RequiredArgsConstructor
public class ConsumptionReportRepository {

    /** Resultado sin el nombre del plan, que está en PostgreSQL. */
    public record Consumption(Integer planId, String contentType, double minutes, int profiles) {
    }

    private final MongoTemplate mongoTemplate;

    public List<Consumption> consumptionByPlanAndType(DateRange range) {
        Document match = new Document("$match", new Document("startedAt", new Document()
                .append("$gte", Date.from(range.startInstant()))
                .append("$lt", Date.from(range.endExclusiveInstant()))));

        Document group = new Document("$group", new Document()
                .append("_id", new Document("planId", "$planId").append("contentType", "$contentType"))
                .append("watchedSec", new Document("$sum", "$watchedSec"))
                .append("profiles", new Document("$addToSet", "$profileId")));

        Document project = new Document("$project", new Document()
                .append("_id", 0)
                .append("planId", "$_id.planId")
                .append("contentType", "$_id.contentType")
                .append("minutes", new Document("$round",
                        List.of(new Document("$divide", List.of("$watchedSec", 60)), 1)))
                .append("profiles", new Document("$size", "$profiles")));

        Document sort = new Document("$sort", new Document("planId", 1).append("contentType", 1));

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.stage(match), Aggregation.stage(group),
                Aggregation.stage(project), Aggregation.stage(sort));

        return mongoTemplate.aggregate(aggregation, "viewingSessions", Document.class).getMappedResults().stream()
                .map(d -> new Consumption(
                        ((Number) d.get("planId")).intValue(),
                        d.getString("contentType"),
                        ((Number) d.get("minutes")).doubleValue(),
                        ((Number) d.get("profiles")).intValue()))
                .toList();
    }
}
