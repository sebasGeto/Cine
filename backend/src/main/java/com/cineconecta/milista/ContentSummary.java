package com.cineconecta.milista;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vista de solo lectura de un contenido con los campos que Mi lista muestra.
 * No es un @Document: se lee con MongoTemplate sobre la colección "contents"
 * para no chocar con la clase Content de C, que es la dueña del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class ContentSummary {

    public static final String COLLECTION = "contents";
    public static final String PUBLISHED = "published";
    public static final String RETIRED = "retired";

    @Id
    private ObjectId id;
    private String type;
    private String title;
    private Integer releaseYear;
    private Integer ageRating;
    private String coverUrl;
    private String status;

    /** Ni publicado ni retirado: un borrador que el cliente no debe ver. */
    public static boolean isDraft(ContentSummary c) {
        return !PUBLISHED.equals(c.getStatus()) && !RETIRED.equals(c.getStatus());
    }
}
