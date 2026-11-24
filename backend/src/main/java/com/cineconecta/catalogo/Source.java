package com.cineconecta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fuente o institución de un documental. kind: institution, archive o publication. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    private String name;
    private String kind;
    private String url;
}
