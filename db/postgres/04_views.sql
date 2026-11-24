-- =====================================================================
-- 04_views.sql
-- Vistas (sección 3.5 del documento de la entrega).
-- Una vista es una consulta guardada con nombre: se consulta como una
-- tabla, pero no almacena datos. También sirven como mecanismo de
-- seguridad: a un rol se le puede dar acceso a la vista y no a las tablas.
-- =====================================================================


-- ---------------------------------------------------------------------
-- v_current_subscriptions (RF-13)
-- Suscripción vigente de cada cuenta, con los datos de su plan.
-- Vigente = startsAt <= ahora < endsAt (en UTC).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_current_subscriptions AS
SELECT s.accountId,
       s.subscriptionId,
       s.planId,
       p.name AS planName,
       p.maxProfiles,
       s.startsAt,
       s.endsAt
FROM subscriptions s
JOIN plans p ON p.planId = s.planId
WHERE s.startsAt <= (now() AT TIME ZONE 'UTC')
  AND (now() AT TIME ZONE 'UTC') < s.endsAt;


-- ---------------------------------------------------------------------
-- v_account_history (RF-04)
-- Historial de suscripciones de cada cuenta con su pago y su plan.
-- isCurrent indica si esa suscripción es la vigente.
-- El orden por fecha se aplica al consultarla:
--   SELECT * FROM v_account_history WHERE accountId = ? ORDER BY startsAt DESC;
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_account_history AS
SELECT s.accountId,
       s.subscriptionId,
       p.name AS planName,
       s.startsAt,
       s.endsAt,
       (s.startsAt <= (now() AT TIME ZONE 'UTC')
        AND (now() AT TIME ZONE 'UTC') < s.endsAt) AS isCurrent,
       pay.paymentId,
       pay.paidAt,
       pay.amount,
       pay.reference
FROM subscriptions s
JOIN plans p      ON p.planId = s.planId
JOIN payments pay ON pay.subscriptionId = s.subscriptionId;


-- ---------------------------------------------------------------------
-- v_revenue_by_plan (RF-35, base del informe de ingresos)
-- Cada pago con su suscripción y su plan. El informe filtra por paidAt
-- y agrupa por plan:
--   SELECT planId, planName, SUM(amount) AS total, COUNT(*) AS subscriptions
--   FROM v_revenue_by_plan
--   WHERE paidAt >= :desde AND paidAt < :hasta
--   GROUP BY planId, planName;
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_revenue_by_plan AS
SELECT pay.paymentId,
       pay.paidAt,
       pay.amount,
       s.subscriptionId,
       s.accountId,
       p.planId,
       p.name AS planName
FROM payments pay
JOIN subscriptions s ON s.subscriptionId = pay.subscriptionId
JOIN plans p         ON p.planId = s.planId;
