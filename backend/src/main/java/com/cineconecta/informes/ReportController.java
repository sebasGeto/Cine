package com.cineconecta.informes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Informes para la administración. Solo ADMIN (por la ruta /api/admin).
 * Fechas AAAA-MM-DD en UTC, ambas incluidas. Con format=csv se descarga un archivo (RF-37).
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reportService;

    /** RF-35. */
    @GetMapping("/revenue")
    public ResponseEntity<?> revenue(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
                                     @RequestParam(defaultValue = "json") String format) {
        boolean csv = isCsv(format);
        DateRange range = new DateRange(from, to);
        return respond(reportService.revenue(range), csv, "ingresos-por-plan", range,
                List.of("planId", "planName", "total", "subscriptions"),
                r -> List.of(r.planId(), r.planName(), r.total(), r.subscriptions()));
    }

    /** RF-36. */
    @GetMapping("/consumption")
    public ResponseEntity<?> consumption(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "json") String format) {
        boolean csv = isCsv(format);
        DateRange range = new DateRange(from, to);
        return respond(reportService.consumption(range), csv, "consumo-por-plan-y-tipo", range,
                List.of("planId", "planName", "contentType", "minutes", "profiles"),
                r -> List.of(r.planId(), r.planName(), r.contentType(), r.minutes(), r.profiles()));
    }

    private static boolean isCsv(String format) {
        return switch (format) {
            case "csv" -> true;
            case "json" -> false;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FORMAT",
                    "El formato debe ser json o csv");
        };
    }

    private static <T> ResponseEntity<?> respond(List<T> rows, boolean csv, String name, DateRange range,
                                                 List<String> header, Function<T, List<Object>> toCells) {
        if (!csv) {
            return ResponseEntity.ok(rows);
        }
        String filename = name + "_" + range.from() + "_" + range.to() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(TEXT_CSV)
                .body(CsvWriter.write(header, rows.stream().map(toCells).toList()));
    }
}
