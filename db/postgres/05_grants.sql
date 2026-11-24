-- =====================================================================
-- 05_grants.sql
-- Permisos por rol (sección 3.7 del documento de la entrega).
-- Principio de mínimo privilegio, con el patrón visto en clase:
--   dueño (owner)   -> cine_admin (POSTGRES_USER): crea los objetos;
--                      solo lo usan los scripts de creación.
--   escritura       -> cineconecta_app: el usuario de la aplicación.
--   lectura         -> cineconecta_report: solo consulta informes.
-- Los roles se crean en 00_roles.sh (contraseñas desde el .env).
-- =====================================================================

-- Por defecto PostgreSQL deja que cualquier rol (PUBLIC) ejecute funciones.
-- Se quita ese permiso y se otorga solo a quien lo necesita.
REVOKE ALL ON FUNCTION fn_current_subscription(INTEGER, TIMESTAMP) FROM PUBLIC;
REVOKE ALL ON FUNCTION sp_purchase_subscription(INTEGER, INTEGER, BOOLEAN, VARCHAR, BOOLEAN) FROM PUBLIC;

-- ---------------------------------------------------------------------
-- cineconecta_app (aplicación)
-- ---------------------------------------------------------------------
GRANT CONNECT ON DATABASE cineconecta TO cineconecta_app;
GRANT USAGE ON SCHEMA public TO cineconecta_app;

-- Cuentas y perfiles: la aplicación los crea y actualiza (sin DELETE)
GRANT SELECT, INSERT, UPDATE ON accounts, profiles TO cineconecta_app;

-- Planes, suscripciones y pagos: solo lectura.
-- Suscripciones y pagos se crean ÚNICAMENTE con sp_purchase_subscription.
GRANT SELECT ON plans, subscriptions, payments TO cineconecta_app;

-- Vistas que usa la aplicación (suscripción vigente e historial de la cuenta)
GRANT SELECT ON v_current_subscriptions, v_account_history TO cineconecta_app;

GRANT EXECUTE ON FUNCTION fn_current_subscription(INTEGER, TIMESTAMP) TO cineconecta_app;
GRANT EXECUTE ON FUNCTION sp_purchase_subscription(INTEGER, INTEGER, BOOLEAN, VARCHAR, BOOLEAN) TO cineconecta_app;

-- ---------------------------------------------------------------------
-- cineconecta_report (informes, solo lectura)
-- ---------------------------------------------------------------------
GRANT CONNECT ON DATABASE cineconecta TO cineconecta_report;
GRANT USAGE ON SCHEMA public TO cineconecta_report;
GRANT SELECT ON v_revenue_by_plan TO cineconecta_report;
