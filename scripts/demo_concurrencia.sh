#!/usr/bin/env bash
# Demo de concurrencia (RF-12): dos contrataciones SIMULTÁNEAS para la misma cuenta.
# Esperado: una responde 201 y la otra 409 (SUBSCRIPTION_ALREADY_ACTIVE o SUBSCRIPTION_OVERLAP),
# y en la base queda una sola suscripción nueva.
#
# Por qué funciona: sp_purchase_subscription bloquea la fila de la cuenta (SELECT ... FOR UPDATE).
# La segunda transacción espera a que la primera termine y entonces ve la suscripción vigente.
# La restricción ex_subs_no_overlap es la última defensa.
#
# Uso: ./scripts/demo_concurrencia.sh [email] [planId]
#   Por defecto: la cuenta de Carla (sin suscripción) y el plan 2 (Estándar).
#   Para repetirla hay que recrear las bases (docker compose down -v && docker compose up -d).
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

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
BODY="{\"planId\":$PLAN_ID,\"paymentApproved\":true}"

echo "Enviando dos contrataciones simultáneas para $EMAIL (plan $PLAN_ID)..."
for i in 1 2; do
  curl -s -o "$TMP/r$i.json" -w '%{http_code}' -X POST "$API/subscriptions" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$BODY" > "$TMP/s$i" &
done
wait

for i in 1 2; do
  echo "Petición $i -> HTTP $(cat "$TMP/s$i")  $(cat "$TMP/r$i.json")"
done

echo
echo "Suscripción vigente después de la demo:"
curl -s "$API/subscriptions/current" -H "Authorization: Bearer $TOKEN"; echo
