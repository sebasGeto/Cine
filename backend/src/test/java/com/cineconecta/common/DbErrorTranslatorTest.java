package com.cineconecta.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLTransientConnectionException;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;

import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;

/** Cada fila de la tabla de errores de la sección 3.1 de la guía, sin necesidad de las bases. */
class DbErrorTranslatorTest {

    private final DbErrorTranslator translator = new DbErrorTranslator();

    /** Formato del protocolo de PostgreSQL: letra del campo + valor, separados por \0. */
    private static PSQLException postgres(String state, String message, String constraint) {
        String fields = "SERROR\0C" + state + "\0M" + message + "\0"
                + (constraint == null ? "" : "n" + constraint + "\0");
        return new PSQLException(new ServerErrorMessage(fields));
    }

    private static MongoWriteException mongo(int code) {
        return new MongoWriteException(new WriteError(code, "error", new BsonDocument()), new ServerAddress(), java.util.Set.of());
    }

    @ParameterizedTest
    @CsvSource({
            "PAYMENT_REJECTED, 402",
            "NO_ACTIVE_SUBSCRIPTION, 403",
            "ACCOUNT_NOT_FOUND, 404",
            "PLAN_NOT_FOUND, 404",
            "SUBSCRIPTION_ALREADY_ACTIVE, 409",
            "TOO_MANY_ACTIVE_PROFILES, 409",
            "PROFILE_LIMIT_REACHED, 409",
            "SIMULATED_FAILURE, 500"
    })
    void traduceReglasDeNegocio(String code, int status) {
        ApiException api = translator.translate(new JpaSystemException(
                new RuntimeException(postgres("P0001", code + ": detalle del error", null))));

        assertThat(api.getStatus().value()).isEqualTo(status);
        assertThat(api.getCode()).isEqualTo(code);
        assertThat(api.getMessage()).isEqualTo("detalle del error");
    }

    @ParameterizedTest
    @CsvSource({
            "23505, ux_accounts_email_ci, 409, EMAIL_ALREADY_EXISTS",
            "23505, ux_profiles_name_ci, 409, PROFILE_NAME_EXISTS",
            "23505, uq_payments_reference, 409, DUPLICATE",
            "23P01, ex_subs_no_overlap, 409, SUBSCRIPTION_OVERLAP",
            "23514, ck_profiles_max_age, 400, INVALID_DATA",
            "40P01, , 409, CONCURRENT_UPDATE",
            "57P01, , 503, DATABASE_UNAVAILABLE",
            "42501, , 500, INTERNAL_ERROR"
    })
    void traduceErroresDePostgres(String state, String constraint, int status, String code) {
        ApiException api = translator.translate(
                new DataIntegrityViolationException("x", postgres(state, "texto interno", constraint)));

        assertThat(api.getStatus().value()).isEqualTo(status);
        assertThat(api.getCode()).isEqualTo(code);
        assertThat(api.getMessage()).doesNotContain("texto interno");
    }

    @Test
    void unCodigoDeNegocioDesconocidoNoSeExpone() {
        ApiException api = translator.translate(postgres("P0001", "OTRO_CODIGO: secreto", null));

        assertThat(api.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(api.getMessage()).doesNotContain("secreto");
    }

    @Test
    void traduceErroresDeMongo() {
        assertThat(translator.translate(new DuplicateKeyException("x", mongo(11000))).getCode()).isEqualTo("DUPLICATE");
        assertThat(translator.translate(new DataIntegrityViolationException("x", mongo(121))).getCode())
                .isEqualTo("INVALID_DOCUMENT");
    }

    @Test
    void baseNoDisponibleEs503() {
        assertThat(translator.translate(new CannotCreateTransactionException("x",
                new SQLTransientConnectionException("timeout"))).getStatus().value()).isEqualTo(503);
        assertThat(translator.translate(new MongoTimeoutException("x")).getStatus().value()).isEqualTo(503);
    }
}
