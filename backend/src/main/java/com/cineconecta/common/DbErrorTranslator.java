package com.cineconecta.common;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;

/**
 * Traduce los errores de PostgreSQL y MongoDB a ApiException.
 *
 * Las reglas de negocio viven en la base (funciones, trigger, restricciones e índices). Aquí no se
 * repiten: solo se lee el error que lanzó la base y se le asigna un código HTTP.
 *
 * Spring y Hibernate envuelven el error original, por eso se recorre la cadena de causas.
 */
@Component
public class DbErrorTranslator {

    /** Mensajes de RAISE EXCEPTION: "CODIGO: texto". */
    private static final Pattern BUSINESS_MESSAGE = Pattern.compile("^([A-Z][A-Z_]+):\\s*(.+)$", Pattern.DOTALL);

    /** SQLState de RAISE EXCEPTION sin código explícito. */
    private static final String RAISE_EXCEPTION = "P0001";

    private static final Map<String, HttpStatus> BUSINESS_STATUS = Map.of(
            "PAYMENT_REJECTED", HttpStatus.PAYMENT_REQUIRED,
            "NO_ACTIVE_SUBSCRIPTION", HttpStatus.FORBIDDEN,
            "ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "PLAN_NOT_FOUND", HttpStatus.NOT_FOUND,
            "SUBSCRIPTION_ALREADY_ACTIVE", HttpStatus.CONFLICT,
            "TOO_MANY_ACTIVE_PROFILES", HttpStatus.CONFLICT,
            "PROFILE_LIMIT_REACHED", HttpStatus.CONFLICT,
            "SIMULATED_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);

    private static final int MONGO_DUPLICATE_KEY = 11000;
    private static final int MONGO_DOCUMENT_VALIDATION = 121;

    public ApiException translate(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof PSQLException e && e.getServerErrorMessage() != null) {
                return fromPostgres(e);
            }
            if (t instanceof MongoException e) {
                return fromMongo(e);
            }
            if (isConnectionFailure(t)) {
                return unavailable();
            }
        }
        return internalError();
    }

    // ------------------------------------------------------------------
    // PostgreSQL
    // ------------------------------------------------------------------

    private ApiException fromPostgres(PSQLException e) {
        // getMessage() antepone "ERROR: "; el mensaje del servidor llega limpio
        ServerErrorMessage error = e.getServerErrorMessage();
        String state = error.getSQLState() == null ? "" : error.getSQLState();

        return switch (state) {
            case RAISE_EXCEPTION -> fromBusinessRule(error.getMessage());
            case "23505" -> fromUniqueViolation(error.getConstraint());
            case "23P01" -> new ApiException(HttpStatus.CONFLICT, "SUBSCRIPTION_OVERLAP",
                    "La cuenta ya tiene una suscripción en ese período");
            // CHECK, NOT NULL, clave foránea
            case "23514", "23502", "23503" -> invalidData();
            // Interbloqueo o conflicto de serialización: la operación se revirtió y se puede reintentar
            case "40001", "40P01" -> new ApiException(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                    "Otra operación modificó los mismos datos; intente de nuevo");
            default -> {
                if (isUnavailableState(state)) {
                    yield unavailable();
                }
                // Clase 22: texto demasiado largo, número fuera de rango, carácter inválido...
                yield state.startsWith("22") ? invalidData() : internalError();
            }
        };
    }

    private ApiException fromBusinessRule(String message) {
        Matcher m = BUSINESS_MESSAGE.matcher(message == null ? "" : message);
        if (m.matches() && BUSINESS_STATUS.containsKey(m.group(1))) {
            return new ApiException(BUSINESS_STATUS.get(m.group(1)), m.group(1), m.group(2));
        }
        return internalError();
    }

    private ApiException fromUniqueViolation(String constraint) {
        return switch (constraint == null ? "" : constraint) {
            case "ux_accounts_email_ci" -> new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "Ya existe una cuenta con ese correo");
            case "ux_profiles_name_ci" -> new ApiException(HttpStatus.CONFLICT, "PROFILE_NAME_EXISTS",
                    "Ya existe un perfil con ese nombre en la cuenta");
            default -> duplicate();
        };
    }

    // ------------------------------------------------------------------
    // MongoDB
    // ------------------------------------------------------------------

    private ApiException fromMongo(MongoException e) {
        if (e instanceof MongoTimeoutException || e instanceof MongoSocketException) {
            return unavailable();
        }
        int code = e.getCode();
        if (e instanceof MongoBulkWriteException bulk && !bulk.getWriteErrors().isEmpty()) {
            code = bulk.getWriteErrors().get(0).getCode();
        }
        return switch (code) {
            case MONGO_DUPLICATE_KEY -> duplicate();
            case MONGO_DOCUMENT_VALIDATION -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT",
                    "El documento no cumple el esquema de la colección");
            default -> internalError();
        };
    }

    // ------------------------------------------------------------------
    // Comunes
    // ------------------------------------------------------------------

    private static boolean isConnectionFailure(Throwable t) {
        // CannotCreateTransactionException: no hubo conexión para abrir la transacción.
        // No es una DataAccessException, por eso se revisa aparte.
        return t instanceof DataAccessResourceFailureException
                || t instanceof CannotCreateTransactionException
                || t instanceof SQLTransientConnectionException
                || (t instanceof SQLException sql && isUnavailableState(sql.getSQLState()));
    }

    /** Clases 08 (conexión), 53 (recursos agotados) y 57P (servidor apagándose o iniciando). */
    private static boolean isUnavailableState(String state) {
        return state != null && (state.startsWith("08") || state.startsWith("53") || state.startsWith("57P"));
    }

    private static ApiException invalidData() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATA",
                "Los datos no cumplen las reglas de la base de datos");
    }

    private static ApiException duplicate() {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE", "El registro ya existe");
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                "La base de datos no está disponible; intente más tarde");
    }

    private static ApiException internalError() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error interno");
    }
}
