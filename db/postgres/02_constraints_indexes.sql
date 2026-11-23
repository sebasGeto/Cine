-- =====================================================================
-- 02_constraints_indexes.sql
-- Restricciones e índices AGREGADOS al modelo suministrado.
-- Ninguno cambia la estructura del modelo: expresan reglas del enunciado
-- (secciones 3.3 y 3.4 del documento de la entrega).
-- =====================================================================

-- Extensión necesaria para combinar "=" (accountId) y "&&" (rangos de fechas)
-- en una misma restricción EXCLUDE.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- PROFILES
-- ---------------------------------------------------------------------
-- isActive solo admite 'Y' (activo) o 'N' (inactivo)
ALTER TABLE profiles
    ADD CONSTRAINT ck_profiles_is_active CHECK (isActive IN ('Y', 'N'));

-- Edad máxima de clasificación permitida para el perfil
ALTER TABLE profiles
    ADD CONSTRAINT ck_profiles_max_age CHECK (maxAgeRating BETWEEN 0 AND 18);

-- ---------------------------------------------------------------------
-- PLANS
-- ---------------------------------------------------------------------
ALTER TABLE plans
    ADD CONSTRAINT ck_plans_price CHECK (price >= 0);

ALTER TABLE plans
    ADD CONSTRAINT ck_plans_max_profiles CHECK (maxProfiles >= 1);

-- ---------------------------------------------------------------------
-- SUBSCRIPTIONS
-- ---------------------------------------------------------------------
-- Toda suscripción dura exactamente 30 días
ALTER TABLE subscriptions
    ADD CONSTRAINT ck_subs_30_days CHECK (endsAt = startsAt + INTERVAL '30 days');

-- Una cuenta no puede tener dos períodos de suscripción superpuestos.
-- tsrange(startsAt, endsAt, '[)') incluye el inicio y excluye el fin,
-- tal como define la vigencia el enunciado. "&&" significa "se traslapan".
-- Es la última defensa si dos contrataciones llegan al mismo tiempo.
ALTER TABLE subscriptions
    ADD CONSTRAINT ex_subs_no_overlap EXCLUDE USING gist (
        accountId WITH =,
        tsrange(startsAt, endsAt, '[)') WITH &&
    );

-- ---------------------------------------------------------------------
-- PAYMENTS
-- ---------------------------------------------------------------------
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_amount CHECK (amount > 0);

-- ---------------------------------------------------------------------
-- ÍNDICES DE APOYO
-- ---------------------------------------------------------------------
-- Suscripción vigente e historial de la cuenta (RF-04, RF-13)
-- y validación de suscripción al reproducir.
CREATE INDEX ix_subs_account_starts ON subscriptions (accountId, startsAt DESC);

-- Clave foránea hacia plans y agrupación por plan en los informes
CREATE INDEX ix_subs_plan ON subscriptions (planId);

-- Filtro por rango de fechas del informe de ingresos (RF-35)
CREATE INDEX ix_payments_paid_at ON payments (paidAt);

-- Conteo de perfiles activos de una cuenta y selección de perfil (RF-09, RF-10).
-- Índice parcial: solo incluye los perfiles activos.
CREATE INDEX ix_profiles_account_active ON profiles (accountId) WHERE isActive = 'Y';
