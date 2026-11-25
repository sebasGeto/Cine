package com.cineconecta.reproduccion;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;

import com.cineconecta.common.ApiException;

/** Errores propios de la reproducción, con el formato {code, message} de ApiException. */
final class PlaybackErrors {

    private PlaybackErrors() {
    }

    static ApiException sessionNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "La sesión de reproducción no existe");
    }

    static ApiException sessionClosed() {
        return new ApiException(HttpStatus.CONFLICT, "SESSION_CLOSED", "La sesión de reproducción ya terminó");
    }

    static ApiException invalidEvent(String message) {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_PLAYBACK_EVENT", message);
    }

    static ApiException concurrentUpdate() {
        return new ApiException(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
            "La sesión cambió mientras se procesaba el evento; intente de nuevo");
    }

    static ObjectId parseId(String text, String name) {
        if (text == null || !ObjectId.isValid(text)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "El valor de '" + name + "' no es válido");
        }
        return new ObjectId(text);
    }

    static ObjectId parseOptionalId(String text, String name) {
        return text == null || text.isBlank() ? null : parseId(text, name);
    }
}
