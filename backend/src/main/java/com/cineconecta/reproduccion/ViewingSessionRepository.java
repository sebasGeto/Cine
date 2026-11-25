package com.cineconecta.reproduccion;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ViewingSessionRepository extends MongoRepository<ViewingSession, ObjectId> {

    /** Una sesión del perfil; la de otro perfil se trata como inexistente. */
    Optional<ViewingSession> findByIdAndProfileId(ObjectId id, Integer profileId);

    /** Historial del perfil, de la más reciente a la más antigua. Usa ix_sessions_profile_started. */
    List<ViewingSession> findByProfileIdOrderByStartedAtDesc(Integer profileId, Pageable page);

    long countByProfileId(Integer profileId);
}
