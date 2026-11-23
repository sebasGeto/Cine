-- =====================================================================
-- 01_schema.sql
-- Tablas del modelo relacional suministrado (Fig. 1 del enunciado).
-- Incluye las claves primarias, foráneas y únicas que ya trae el modelo.
-- Las restricciones e índices que agregamos nosotros van en 02.
--
-- Traducción de tipos Oracle -> PostgreSQL (sin cambiar la estructura):
--   VARCHAR2(n) -> VARCHAR(n)    NUMBER(12,2) -> NUMERIC(12,2)
--   Claves INTEGER generadas con GENERATED ALWAYS AS IDENTITY.
-- Los nombres van sin comillas: PostgreSQL los guarda en minúscula
-- (accountId queda como accountid) y la aplicación los usa igual.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ACCOUNTS: titulares de cuenta (clientes)
-- ---------------------------------------------------------------------
CREATE TABLE accounts (
    accountId     INTEGER GENERATED ALWAYS AS IDENTITY,
    fullName      VARCHAR(150) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    passwordHash  VARCHAR(255) NOT NULL,
    createdAt     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (accountId)
);

-- Un correo por cuenta, sin distinguir mayúsculas y minúsculas
CREATE UNIQUE INDEX ux_accounts_email_ci ON accounts (LOWER(email));

-- ---------------------------------------------------------------------
-- PLANS: planes de suscripción (se cargan por script)
-- ---------------------------------------------------------------------
CREATE TABLE plans (
    planId       INTEGER GENERATED ALWAYS AS IDENTITY,
    name         VARCHAR(60)   NOT NULL,
    price        NUMERIC(12,2) NOT NULL,
    maxProfiles  INTEGER       NOT NULL,
    CONSTRAINT pk_plans PRIMARY KEY (planId),
    CONSTRAINT uq_plans_name UNIQUE (name)
);

-- ---------------------------------------------------------------------
-- PROFILES: perfiles de visualización de una cuenta
-- ---------------------------------------------------------------------
CREATE TABLE profiles (
    profileId     INTEGER GENERATED ALWAYS AS IDENTITY,
    accountId     INTEGER     NOT NULL,
    name          VARCHAR(60) NOT NULL,
    maxAgeRating  INTEGER     NOT NULL,
    isActive      CHAR(1)     NOT NULL,
    CONSTRAINT pk_profiles PRIMARY KEY (profileId),
    CONSTRAINT fk_profiles_account FOREIGN KEY (accountId)
        REFERENCES accounts (accountId) ON DELETE RESTRICT
);

-- Nombre de perfil único dentro de la cuenta, sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_profiles_name_ci ON profiles (accountId, LOWER(name));

-- ---------------------------------------------------------------------
-- SUBSCRIPTIONS: períodos de suscripción de una cuenta a un plan
-- ---------------------------------------------------------------------
CREATE TABLE subscriptions (
    subscriptionId  INTEGER GENERATED ALWAYS AS IDENTITY,
    accountId       INTEGER   NOT NULL,
    planId          INTEGER   NOT NULL,
    startsAt        TIMESTAMP NOT NULL,
    endsAt          TIMESTAMP NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (subscriptionId),
    CONSTRAINT fk_subs_account FOREIGN KEY (accountId)
        REFERENCES accounts (accountId) ON DELETE RESTRICT,
    CONSTRAINT fk_subs_plan FOREIGN KEY (planId)
        REFERENCES plans (planId) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- PAYMENTS: pago (aprobado) asociado a una suscripción
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    paymentId       INTEGER GENERATED ALWAYS AS IDENTITY,
    subscriptionId  INTEGER       NOT NULL,
    paidAt          TIMESTAMP     NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    reference       VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (paymentId),
    CONSTRAINT uq_payments_subscription UNIQUE (subscriptionId),
    CONSTRAINT uq_payments_reference UNIQUE (reference),
    CONSTRAINT fk_payments_subscription FOREIGN KEY (subscriptionId)
        REFERENCES subscriptions (subscriptionId) ON DELETE RESTRICT
);
