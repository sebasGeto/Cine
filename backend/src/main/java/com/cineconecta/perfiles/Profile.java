package com.cineconecta.perfiles;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Perfil de visualización (tabla profiles).
 *
 * @DynamicUpdate: el UPDATE solo incluye las columnas que cambiaron. Así editar el nombre no
 * toca isActive y el trigger trg_profiles_limit (BEFORE UPDATE OF isActive) solo se dispara al
 * activar o desactivar.
 */
@Entity
@Table(name = "profiles")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    public static final String ACTIVE = "Y";
    public static final String INACTIVE = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer profileId;

    /** FK a accounts, sin @ManyToOne para no depender de la entidad de otra área. */
    private Integer accountId;

    /** Único por cuenta sin distinguir mayúsculas (ux_profiles_name_ci). */
    private String name;

    /** 0 a 18 (ck_profiles_max_age). */
    private Integer maxAgeRating;

    /**
     * 'Y' o 'N' (ck_profiles_is_active). Sin @JdbcTypeCode(CHAR) Hibernate espera VARCHAR y la
     * validación del esquema falla con "found bpchar, but expecting char(1)".
     */
    @Column(name = "isActive", columnDefinition = "CHAR(1)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String isActive;

    public boolean active() {
        return ACTIVE.equals(isActive);
    }
}
