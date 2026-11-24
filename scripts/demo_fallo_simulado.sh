#!/usr/bin/env bash
# Demo de atomicidad (RF-12): la función inserta la suscripción y falla ANTES de registrar el pago.
# Esperado: HTTP 500 SIMULATED_FAILURE y ningún registro nuevo en subscriptions ni payments
# (se hizo rollback de toda la transacción). Se comprueba con GET /api/subscriptions/current
# (sigue en 404) y con el historial de la cuenta.
#
# Uso: ./scripts/demo_fallo_simulado.sh [email] [planId]   (por defecto Carla, plan 2)
set -euo pipefail

API="${API:-http://localhost:8080/api}"
EMAIL="${1:-carla@cineconecta.com}"
PLAN_ID="${2:-2}"
PASSWORD="${DEMO_PASSWORD:-Cine2026*}"

TOKEN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "No se pudo iniciar sesión con $EMAIL (¿está corriendo el backend?)" >&2
  exit 1
fi

echo "Contratación con simulateFailure=true:"
curl -s -w '  -> HTTP %{http_code}\n' -X POST "$API/subscriptions" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"planId\":$PLAN_ID,\"paymentApproved\":true,\"simulateFailure\":true}"

echo "Suscripción vigente (debe seguir sin haber):"
curl -s -w '  -> HTTP %{http_code}\n' "$API/subscriptions/current" -H "Authorization: Bearer $TOKEN"

echo "Historial de la cuenta (no debe tener registros nuevos):"
curl -s "$API/account/history" -H "Authorization: Bearer $TOKEN"; echo
