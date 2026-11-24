-- =====================================================================
-- 06_seed.sql
-- Datos de prueba de PostgreSQL.
--
-- Las fechas se calculan RELATIVAS al momento de la carga (en UTC), para
-- que las suscripciones "vigentes" sigan vigentes el día de la demo.
-- Los IDs se fijan a mano (OVERRIDING SYSTEM VALUE) porque los datos de
-- prueba de MongoDB los referencian (accountId, profileId, planId, ...).
--
-- Contraseña de todas las cuentas de prueba: ver README (solo se guarda el hash).
--
-- Cuentas y escenarios de la demo:
--   1 Ana    : suscripción vigente Estándar (2 perfiles) con el CUPO LLENO
--              (Ana y Tomás). Tomás es perfil INFANTIL (edad máxima 7).
--              Historial de 3 suscripciones anteriores.
--   2 Bruno  : suscripción VENCIDA (rechazo por suscripción vencida).
--   3 Carla  : sin suscripción (contratación y concurrencia en la demo).
--   4 Diego  : vigente Premium, 3 perfiles activos y 1 inactivo.
--   5 Elena  : vigente Básico, con historial.
-- =====================================================================

-- Instante de referencia de la carga (UTC, redondeado al minuto)
CREATE TEMP TABLE seed_now AS
SELECT date_trunc('minute', now() AT TIME ZONE 'UTC') AS t;

-- ---------------------------------------------------------------------
-- PLANES
-- ---------------------------------------------------------------------
INSERT INTO plans (planId, name, price, maxProfiles) OVERRIDING SYSTEM VALUE VALUES
    (1, 'Básico',   16900.00, 1),
    (2, 'Estándar', 26900.00, 2),
    (3, 'Premium',  38900.00, 4);

-- ---------------------------------------------------------------------
-- CUENTAS (passwordHash = BCrypt de la contraseña de prueba)
-- ---------------------------------------------------------------------
INSERT INTO accounts (accountId, fullName, email, passwordHash, createdAt) OVERRIDING SYSTEM VALUE
SELECT v.id, v.fullName, v.email,
       '$2a$10$biLFCJcnxBFbhJLxUFWb6OYxz93JzqiBqlrc8eT9rACfhLNcDUnve',
       (SELECT t FROM seed_now) - v.age
FROM (VALUES
    (1, 'Ana Gómez',     'ana@cineconecta.com',   INTERVAL '120 days'),
    (2, 'Bruno Díaz',    'bruno@cineconecta.com', INTERVAL '90 days'),
    (3, 'Carla Ruiz',    'carla@cineconecta.com', INTERVAL '3 days'),
    (4, 'Diego Torres',  'diego@cineconecta.com', INTERVAL '60 days'),
    (5, 'Elena Vargas',  'elena@cineconecta.com', INTERVAL '75 days')
) AS v(id, fullName, email, age);

-- ---------------------------------------------------------------------
-- SUSCRIPCIONES (30 días cada una, sin traslapes por cuenta)
-- start = momento de la carga - "hace"
-- ---------------------------------------------------------------------
INSERT INTO subscriptions (subscriptionId, accountId, planId, startsAt, endsAt) OVERRIDING SYSTEM VALUE
SELECT v.id, v.accountId, v.planId,
       (SELECT t FROM seed_now) - v.ago,
       (SELECT t FROM seed_now) - v.ago + INTERVAL '30 days'
FROM (VALUES
    -- Ana: historial y vigente (Estándar)
    ( 1, 1, 1, INTERVAL '95 days'),
    ( 2, 1, 2, INTERVAL '65 days'),
    ( 3, 1, 2, INTERVAL '35 days'),
    ( 4, 1, 2, INTERVAL '5 days'),    -- vigente
    -- Bruno: solo suscripciones vencidas
    ( 5, 2, 1, INTERVAL '70 days'),
    ( 6, 2, 1, INTERVAL '40 days'),   -- venció hace 10 días
    -- Diego: historial y vigente (Premium)
    ( 7, 4, 2, INTERVAL '42 days'),
    ( 8, 4, 3, INTERVAL '12 days'),   -- vigente
    -- Elena: historial y vigente (Básico)
    ( 9, 5, 1, INTERVAL '62 days'),
    (10, 5, 1, INTERVAL '32 days'),
    (11, 5, 1, INTERVAL '2 days')     -- vigente
) AS v(id, accountId, planId, ago);

-- ---------------------------------------------------------------------
-- PAGOS (uno por suscripción, con el precio del plan)
-- ---------------------------------------------------------------------
INSERT INTO payments (paymentId, subscriptionId, paidAt, amount, reference) OVERRIDING SYSTEM VALUE
SELECT s.subscriptionId, s.subscriptionId, s.startsAt, p.price,
       'PAY-SEED-' || LPAD(s.subscriptionId::TEXT, 4, '0')
FROM subscriptions s
JOIN plans p ON p.planId = s.planId;

-- ---------------------------------------------------------------------
-- PERFILES
-- Se desactiva el trigger de cupo solo durante esta carga histórica:
-- el perfil de Bruno se creó cuando su suscripción estaba vigente.
-- ---------------------------------------------------------------------
ALTER TABLE profiles DISABLE TRIGGER trg_profiles_limit;

INSERT INTO profiles (profileId, accountId, name, maxAgeRating, isActive) OVERRIDING SYSTEM VALUE VALUES
    (1, 1, 'Ana',    18, 'Y'),
    (2, 1, 'Tomás',   7, 'Y'),   -- perfil infantil; cupo de Ana lleno (2/2)
    (3, 2, 'Bruno',  18, 'Y'),
    (4, 4, 'Diego',  18, 'Y'),
    (5, 4, 'Laura',  16, 'Y'),
    (6, 4, 'Sofía',  12, 'Y'),
    (7, 4, 'Abuelo', 18, 'N'),   -- inactivo
    (8, 5, 'Elena',  18, 'Y');

ALTER TABLE profiles ENABLE TRIGGER trg_profiles_limit;

-- ---------------------------------------------------------------------
-- Las identidades continúan después de los IDs fijados a mano
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('accounts', 'accountid'),           (SELECT MAX(accountId) FROM accounts));
SELECT setval(pg_get_serial_sequence('plans', 'planid'),                 (SELECT MAX(planId) FROM plans));
SELECT setval(pg_get_serial_sequence('subscriptions', 'subscriptionid'), (SELECT MAX(subscriptionId) FROM subscriptions));
SELECT setval(pg_get_serial_sequence('payments', 'paymentid'),           (SELECT MAX(paymentId) FROM payments));
SELECT setval(pg_get_serial_sequence('profiles', 'profileid'),           (SELECT MAX(profileId) FROM profiles));

DROP TABLE seed_now;
