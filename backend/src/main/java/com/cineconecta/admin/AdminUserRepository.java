package com.cineconecta.admin;

import java.time.Instant;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface AdminUserRepository extends MongoRepository<AdminUser, ObjectId> {

    /**
     * Administrador activo por usuario o correo, sin distinguir mayúsculas.
     * Usa la misma collation de ux_admin_username y ux_admin_email: con otra (o con una
     * expresión regular) MongoDB no podría usar esos índices.
     */
    @Query(value = "{ $or: [ { username: ?0 }, { email: ?0 } ], isActive: true }",
            collation = "{ locale: 'es', strength: 2 }")
    Optional<AdminUser> findActiveByUsernameOrEmail(String login);

    /** Cambia solo lastLoginAt, sin reescribir el documento. */
    @Query("{ _id: ?0 }")
    @Update("{ $set: { lastLoginAt: ?1 } }")
    void updateLastLogin(ObjectId id, Instant at);
}
