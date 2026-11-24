package com.cineconecta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persona entrevistada en un documental. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Interviewee {

    private String name;
    private String description;
}
