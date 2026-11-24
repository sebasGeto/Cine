package com.cineconecta.reproduccion;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ViewingProgressRepository extends MongoRepository<ViewingProgress, ObjectId> {

    /** Usa ux_progress_profile_content. */
    Optional<ViewingProgress> findByProfileIdAndContentId(Integer profileId, ObjectId contentId);

    /** "Continuar viendo": no completados, del más reciente al más antiguo. Usa ix_progress_continue. */
    List<ViewingProgress> findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(Integer profileId, Pageable page);
}
