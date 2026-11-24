package com.cineconecta.catalogo;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Operaciones simples sobre contents. Búsquedas y actualizaciones parciales: ContentStore. */
public interface ContentRepository extends MongoRepository<Content, ObjectId> {
}
