package com.cineconecta.cuentas;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuenta de un cliente (tabla accounts de PostgreSQL). */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId;

    private String fullName;

    /** Se guarda en minúscula; la unicidad la garantiza el índice ux_accounts_email_ci. */
    private String email;

    /** Hash BCrypt; la contraseña nunca se guarda. */
    private String passwordHash;

    /** En UTC. */
    private LocalDateTime createdAt;
}
