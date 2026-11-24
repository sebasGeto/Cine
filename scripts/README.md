# Scripts de apoyo

Scripts de la demo de contratación (RF-12). Necesitan las bases y el backend corriendo
(`docker compose up -d` y `CineconectaApplication`). Usan `curl` y la contraseña de las cuentas de prueba.

| Script | Qué muestra | Resultado esperado |
|---|---|---|
| `demo_concurrencia.sh` | Dos `POST /api/subscriptions` simultáneos para Carla | Uno `201`, el otro `409`; una sola suscripción en la base |
| `demo_fallo_simulado.sh` | Contratación con `simulateFailure: true` | `500 SIMULATED_FAILURE`; sin registros en `subscriptions` ni `payments` |

```bash
./scripts/demo_fallo_simulado.sh   # primero: Carla sigue sin suscripción
./scripts/demo_concurrencia.sh     # después: Carla queda con una sola suscripción
```

Para repetir la demo desde cero: `docker compose down -v && docker compose up -d`.

Comprobación en la base (DBeaver/pgAdmin, cuenta de Carla = 3):

```sql
SELECT s.subscriptionId, s.startsAt, p.paymentId, p.reference
FROM subscriptions s LEFT JOIN payments p ON p.subscriptionId = s.subscriptionId
WHERE s.accountId = 3;
```
