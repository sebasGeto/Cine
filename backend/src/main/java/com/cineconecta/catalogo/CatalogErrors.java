package com.cineconecta.catalogo;

import org.springframework.http.HttpStatus;

import com.cineconecta.common.ApiException;

/** Errores del catálogo, para que cada código tenga un solo mensaje y un solo estado HTTP. */
final class CatalogErrors {

    private CatalogErrors() {
    }

    /** Administración: el id no existe. */
    static ApiException contentNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "El contenido no existe");
    }

    /** Cliente: no existe o no está publicado (no se dice cuál). */
    static ApiException contentNotAvailable() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_AVAILABLE", "El contenido no está disponible");
    }

    static ApiException ageRatingNotAllowed() {
        return new ApiException(HttpStatus.FORBIDDEN, "AGE_RATING_NOT_ALLOWED",
                "La clasificación del contenido no está permitida para este perfil");
    }

    static ApiException episodeRequired() {
        return new ApiException(HttpStatus.BAD_REQUEST, "EPISODE_REQUIRED",
                "Las series se reproducen por episodio: indique cuál");
    }

    static ApiException seasonNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "SEASON_NOT_FOUND", "La temporada no existe");
    }

    static ApiException episodeNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "EPISODE_NOT_FOUND", "El episodio no existe");
    }

    static ApiException notASeries() {
        return new ApiException(HttpStatus.BAD_REQUEST, "NOT_A_SERIES",
                "Solo las series tienen temporadas y episodios");
    }

    static ApiException seasonNumberExists() {
        return new ApiException(HttpStatus.CONFLICT, "SEASON_NUMBER_EXISTS",
                "La serie ya tiene una temporada con ese número");
    }

    static ApiException episodeNumberExists() {
        return new ApiException(HttpStatus.CONFLICT, "EPISODE_NUMBER_EXISTS",
                "La temporada ya tiene un episodio con ese número");
    }

    static ApiException typeCannotChange() {
        return new ApiException(HttpStatus.BAD_REQUEST, "TYPE_CANNOT_CHANGE",
                "El tipo de un contenido no se puede cambiar");
    }

    static ApiException emptySeasonConflict() {
        return new ApiException(HttpStatus.CONFLICT, "EMPTY_SEASON_CONFLICT",
                "Otra serie ya tiene una temporada sin episodios; envíe la temporada con al menos un episodio");
    }

    static ApiException invalidParameter(String name) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor de '" + name + "' no es válido");
    }
}
