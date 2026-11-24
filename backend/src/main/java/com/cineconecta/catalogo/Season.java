package com.cineconecta.catalogo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Temporada embebida en su serie. El número no se repite dentro de la serie. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season {

    private Integer number;
    private String description;
    private List<Episode> episodes;
}
