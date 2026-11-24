package com.cineconecta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Integrante del reparto de una película. character solo cuando se conoce el personaje. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CastMember {

    private String name;
    private String character;
}
