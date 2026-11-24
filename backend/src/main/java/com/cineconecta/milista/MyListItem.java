package com.cineconecta.milista;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un contenido guardado en "Mi lista" de un perfil (colección myList, RI-10).
 * Un documento por par perfil-contenido; el índice único ux_mylist_profile_content
 * impide duplicados (RF-24). accountId y profileId son referencias a PostgreSQL.
 */
@Document(collection = "myList")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyListItem {

    @Id
    private ObjectId id;

    private Integer accountId;
    private Integer profileId;
    private ObjectId contentId;
    private Instant addedAt;
}
