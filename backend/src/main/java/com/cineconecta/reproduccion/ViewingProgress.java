package com.cineconecta.reproduccion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado actual de un perfil frente a un contenido (colección viewingProgress, RI-12).
 * Un solo documento por perfil y contenido (índice único ux_progress_profile_content).
 *
 * A diferencia de viewingSessions, este documento se sobrescribe: es la posición en la que se
 * retoma y alimenta "Continuar viendo" (RF-31). El historial completo queda en las sesiones.
 */
@Document(ViewingProgress.COLLECTION)
@Getter
@Setter
@NoArgsConstructor
public class ViewingProgress {

    public static final String COLLECTION = "viewingProgress";

    @Id
    private ObjectId id;

    private Integer accountId;
    private Integer profileId;
    private ObjectId contentId;
    private String contentType;

    /** Posición en la que se retoma (en series, el episodio en curso). */
    private Current current;
    private Boolean completed;
    private Instant lastActivityAt;

    /** Solo series: progreso de cada episodio visto. */
    private List<EpisodeProgress> episodes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Current {
        private Integer positionSec;
        private Integer durationSec;
        private ObjectId episodeId;
        private Integer seasonNumber;
        private Integer episodeNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpisodeProgress {
        private ObjectId episodeId;
        private Integer positionSec;
        private Boolean completed;
        private Instant updatedAt;
    }

    List<EpisodeProgress> episodesOrEmpty() {
        if (episodes == null) {
            episodes = new ArrayList<>();
        }
        return episodes;
    }
}
