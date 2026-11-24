package com.cineconecta.milista;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MyListRepository extends MongoRepository<MyListItem, ObjectId> {

    /** Lista del perfil, del más reciente al más antiguo. Usa ix_mylist_profile_added. */
    List<MyListItem> findByProfileIdOrderByAddedAtDesc(Integer profileId);

    /** Usa ux_mylist_profile_content. Devuelve cuántos documentos borró (0 o 1). */
    long deleteByProfileIdAndContentId(Integer profileId, ObjectId contentId);
}
