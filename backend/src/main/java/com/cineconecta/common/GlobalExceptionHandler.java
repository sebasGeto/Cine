package com.cineconecta.common;

import java.util.stream.Collectors;

import com.mongodb.MongoException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte cualquier excepción en {"code", "message"} con el código HTTP que corresponde.
 * Nunca se devuelve al cliente SQL, trazas ni detalles internos: eso solo queda en el log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final DbErrorTranslator dbErrorTranslator;

    public GlobalExceptionHandler(DbErrorTranslator dbErrorTranslator) {
        this.dbErrorTranslator = dbErrorTranslator;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    /**
     * Errores de las bases. Llegan aquí después del rollback: los servicios no los capturan.
     * Además de DataAccessException se cubren los que no heredan de ella (sin conexión para abrir
     * la transacción, EntityManager usado directamente y MongoTemplate sin traducir).
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class,
            PersistenceException.class, MongoException.class})
    public ResponseEntity<Object> handleDatabase(Exception ex) {
        ApiException api = dbErrorTranslator.translate(ex);
        if (api.getStatus().is5xxServerError()) {
            log.error("Error de base de datos ({})", api.getCode(), ex);
        }
        return handleApi(api);
    }

    /** Las decisiones de seguridad las responde Spring Security (401/403); aquí no se convierten en 500. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurity(RuntimeException ex) {
        throw ex;
    }

    /** Parámetro o variable de ruta con un tipo que no corresponde (por ejemplo, una fecha mal escrita). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor de '" + ex.getName() + "' no es válido");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error interno");
    }

    /** Errores propios de Spring MVC: validación, JSON mal formado, parámetros faltantes, 404, 405... */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ResponseEntity<Object> response = switch (ex) {
            case MethodArgumentNotValidException e -> build(status, "VALIDATION_ERROR", fieldErrors(e));
            case HttpMessageNotReadableException e ->
                    build(status, "INVALID_JSON", "El cuerpo de la petición no es válido");
            case MissingServletRequestParameterException e ->
                    build(status, "MISSING_PARAMETER", "Falta el parámetro '" + e.getParameterName() + "'");
            case MissingRequestHeaderException e ->
                    build(status, "MISSING_HEADER", "Falta el encabezado '" + e.getHeaderName() + "'");
            default -> build(status, status.name(), defaultMessage(status));
        };
        if (status.is5xxServerError()) {
            log.error("Error al procesar la petición", ex);
        }
        // Conserva encabezados como Allow en los 405
        return ResponseEntity.status(status).headers(headers)
                .contentType(MediaType.APPLICATION_JSON).body(response.getBody());
    }

    private static String fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
    }

    private static String defaultMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "La ruta solicitada no existe";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido en esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "El contenido debe enviarse como application/json";
            case BAD_REQUEST -> "La petición no es válida";
            default -> "No se pudo procesar la petición";
        };
    }

    /** El tipo se fija en JSON para que el error se entregue aunque el cliente pida text/csv. */
    private static ResponseEntity<Object> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(code, message));
    }
}
