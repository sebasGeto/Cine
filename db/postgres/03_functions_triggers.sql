-- =====================================================================
-- 03_functions_triggers.sql
-- Funciones y triggers (secciones 3.3 y 3.6 del documento de la entrega).
--
-- Control de concurrencia: bloqueo pesimista con SELECT ... FOR UPDATE
-- sobre la fila de la cuenta. La segunda transacción que pida el mismo
-- bloqueo espera hasta que la primera haga COMMIT o ROLLBACK (los bloqueos
-- se liberan al terminar la transacción, como en el bloqueo en dos fases).
--
-- Todas las fechas se manejan en UTC: (now() AT TIME ZONE 'UTC').
-- =====================================================================


-- ---------------------------------------------------------------------
-- fn_current_subscription
-- Devuelve la suscripción vigente de una cuenta en el instante p_at,
-- o ninguna fila si no tiene. Vigente = startsAt <= p_at < endsAt.
-- La usan el trigger de perfiles, la contratación y la validación
-- de reproducción.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_current_subscription(
    p_account_id INTEGER,
    p_at         TIMESTAMP DEFAULT (now() AT TIME ZONE 'UTC')
)
RETURNS TABLE (subscriptionId INTEGER, planId INTEGER, maxProfiles INTEGER, endsAt TIMESTAMP)
LANGUAGE sql
STABLE
AS $$
    SELECT s.subscriptionId, s.planId, p.maxProfiles, s.endsAt
    FROM subscriptions s
    JOIN plans p ON p.planId = s.planId
    WHERE s.accountId = p_account_id
      AND s.startsAt <= p_at
      AND p_at < s.endsAt;
$$;


-- ---------------------------------------------------------------------
-- trg_profiles_limit  (RF-05 crear perfil, RF-08 reactivar perfil)
-- Cuando un perfil queda ACTIVO (se crea activo o pasa de 'N' a 'Y'):
--   1. Bloquea la cuenta (FOR UPDATE) para que dos creaciones
--      simultáneas no superen el cupo.
--   2. Exige suscripción vigente.
--   3. Exige que los perfiles activos no superen maxProfiles del plan.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_profiles_limit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_max_profiles INTEGER;
    v_active       INTEGER;
BEGIN
    -- Solo interesa cuando el perfil queda activo
    IF NEW.isActive <> 'Y' THEN
        RETURN NEW;
    END IF;
    -- Si ya estaba activo (por ejemplo, solo se cambió el nombre), no se valida otra vez
    IF TG_OP = 'UPDATE' AND OLD.isActive = 'Y' THEN
        RETURN NEW;
    END IF;

    -- 1. Bloqueo de la cuenta: otra transacción sobre la misma cuenta espera aquí
    PERFORM 1 FROM accounts WHERE accountId = NEW.accountId FOR UPDATE;

    -- 2. Suscripción vigente
    SELECT cs.maxProfiles INTO v_max_profiles
    FROM fn_current_subscription(NEW.accountId) cs;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'NO_ACTIVE_SUBSCRIPTION: la cuenta % no tiene suscripción vigente', NEW.accountId;
    END IF;

    -- 3. Cupo de perfiles activos (sin contar el perfil que se está activando)
    SELECT COUNT(*) INTO v_active
    FROM profiles
    WHERE accountId = NEW.accountId
      AND isActive = 'Y'
      AND profileId <> NEW.profileId;

    IF v_active >= v_max_profiles THEN
        RAISE EXCEPTION 'PROFILE_LIMIT_REACHED: el plan vigente permite máximo % perfiles activos', v_max_profiles;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_profiles_limit
    BEFORE INSERT OR UPDATE OF isActive ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION fn_profiles_limit();


-- ---------------------------------------------------------------------
-- sp_purchase_subscription  (RF-12 contratar suscripción)
-- Todo ocurre en UNA transacción: si cualquier paso falla, se hace
-- ROLLBACK y no queda ni la suscripción ni el pago (atomicidad).
--   1. Bloquea la cuenta (FOR UPDATE): una segunda solicitud simultánea
--      para la misma cuenta espera a que esta termine.
--   2. Rechaza si el pago no fue aprobado (no se guardan intentos rechazados).
--   3. Rechaza si ya hay una suscripción vigente.
--   4. Rechaza si los perfiles activos superan maxProfiles del nuevo plan.
--   5. Inserta la suscripción: startsAt = ahora, endsAt = ahora + 30 días.
--   6. Inserta el pago con el precio del plan.
-- Devuelve el subscriptionId creado.
--
-- p_simulate_failure (solo para la demo): lanza un error después de
-- insertar la suscripción y antes del pago, para mostrar que no queda
-- ningún registro parcial.
--
-- SECURITY DEFINER: se ejecuta con los permisos del dueño de la función.
-- Así el usuario de la aplicación NO tiene INSERT directo sobre
-- subscriptions ni payments; solo puede crearlos a través de esta función.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_purchase_subscription(
    p_account_id        INTEGER,
    p_plan_id           INTEGER,
    p_payment_approved  BOOLEAN,
    p_reference         VARCHAR,
    p_simulate_failure  BOOLEAN DEFAULT FALSE
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_now             TIMESTAMP := (now() AT TIME ZONE 'UTC');
    v_price           NUMERIC(12,2);
    v_max_profiles    INTEGER;
    v_active          INTEGER;
    v_subscription_id INTEGER;
BEGIN
    -- 1. Bloqueo de la cuenta
    PERFORM 1 FROM accounts WHERE accountId = p_account_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'ACCOUNT_NOT_FOUND: la cuenta % no existe', p_account_id;
    END IF;

    -- 2. Pago simulado
    IF NOT p_payment_approved THEN
        RAISE EXCEPTION 'PAYMENT_REJECTED: el pago fue rechazado';
    END IF;

    -- Datos del plan
    SELECT price, maxProfiles INTO v_price, v_max_profiles
    FROM plans
    WHERE planId = p_plan_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'PLAN_NOT_FOUND: el plan % no existe', p_plan_id;
    END IF;

    -- 3. Sin suscripción vigente
    IF EXISTS (SELECT 1 FROM fn_current_subscription(p_account_id, v_now)) THEN
        RAISE EXCEPTION 'SUBSCRIPTION_ALREADY_ACTIVE: la cuenta % ya tiene una suscripción vigente', p_account_id;
    END IF;

    -- 4. Perfiles activos dentro del cupo del nuevo plan
    SELECT COUNT(*) INTO v_active
    FROM profiles
    WHERE accountId = p_account_id
      AND isActive = 'Y';
    IF v_active > v_max_profiles THEN
        RAISE EXCEPTION 'TOO_MANY_ACTIVE_PROFILES: hay % perfiles activos y el plan permite %; desactive los sobrantes',
            v_active, v_max_profiles;
    END IF;

    -- 5. Suscripción
    INSERT INTO subscriptions (accountId, planId, startsAt, endsAt)
    VALUES (p_account_id, p_plan_id, v_now, v_now + INTERVAL '30 days')
    RETURNING subscriptionId INTO v_subscription_id;

    -- Fallo simulado para la demo: la suscripción insertada se revierte
    IF p_simulate_failure THEN
        RAISE EXCEPTION 'SIMULATED_FAILURE: fallo simulado antes de registrar el pago';
    END IF;

    -- 6. Pago
    INSERT INTO payments (subscriptionId, paidAt, amount, reference)
    VALUES (v_subscription_id, v_now, v_price, p_reference);

    RETURN v_subscription_id;
END;
$$;
