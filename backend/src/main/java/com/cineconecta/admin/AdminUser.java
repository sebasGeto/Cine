package com.cineconecta.admin;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Administrador (colección adminUsers de MongoDB), independiente de las cuentas de clientes. */
@Document("adminUsers")
@Getter
@Setter
@NoArgsConstructor
public class AdminUser {

    @Id
    private ObjectId id;

    private String username;
    private String email;
    private String fullName;
    private String passwordHash;
    private String role;
    private Boolean isActive;
    private Instant createdAt;
    private Instant lastLoginAt;
}
