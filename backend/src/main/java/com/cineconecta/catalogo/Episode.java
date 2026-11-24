package com.cineconecta.catalogo;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Episodio embebido en su temporada. episodeId lo genera el servidor y no cambia nunca. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Episode {

    private ObjectId episodeId;
    private Integer number;
    private String title;
    private String synopsis;
    private Integer durationSec;
}
