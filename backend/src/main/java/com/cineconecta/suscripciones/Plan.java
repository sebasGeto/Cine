package com.cineconecta.suscripciones;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Plan de suscripción (tabla plans). Se carga por script: la aplicación solo lo lee. */
@Entity
@Table(name = "plans")
@Immutable
@Getter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planId;

    private String name;

    private BigDecimal price;

    private Integer maxProfiles;
}
