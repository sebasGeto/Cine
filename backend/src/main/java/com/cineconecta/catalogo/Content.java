package com.cineconecta.catalogo;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Película, serie o documental (colección contents de MongoDB).
 *
 * Es un catálogo polimórfico: los campos comunes son obligatorios y los propios de cada tipo
 * quedan en null en los demás (MongoDB no guarda los null). El validador $jsonSchema de la
 * colección es la última defensa.
 *
 * Sirve para insertar y leer. Las actualizaciones se hacen con $set sobre campos concretos
 * (ContentStore), nunca reescribiendo el documento: así no se pierden campos que se agreguen
 * al catálogo en el futuro ni cambios simultáneos.
 */
@Document(Content.COLLECTION)
@Getter
@Setter
@NoArgsConstructor
public class Content {

    public static final String COLLECTION = "contents";

    @Id
    private ObjectId id;

    private String type;
    private String title;
    private String synopsis;
    private Integer releaseYear;
    private List<String> genres;
    private List<String> countries;
    private List<String> audioLanguages;
    private List<String> subtitleLanguages;
    private Integer ageRating;
    private String coverUrl;
    private String status;
    private Instant publishedAt;
    private Instant retiredAt;
    private Integer schemaVersion;
    private Instant createdAt;
    private Instant updatedAt;

    // Película y documental
    private Integer durationSec;
    private List<String> directors;

    // Película
    private List<CastMember> cast;

    // Documental
    private List<String> topics;
    private List<Interviewee> interviewees;
    private List<Source> sources;

    // Serie
    private List<Season> seasons;

    public boolean isSeries() {
        return ContentType.SERIES.equals(type);
    }

    public boolean isPublished() {
        return ContentStatus.PUBLISHED.equals(status);
    }
}
