# CineConecta

Plataforma web de acceso por suscripción a películas, series y documentales, con **persistencia políglota**.
Proyecto final de Sistemas de Datos Intensivos II.

| Base de datos | Responsabilidad |
|---|---|
| **PostgreSQL** (fuente de verdad) | Cuentas, perfiles, planes, suscripciones y pagos |
| **MongoDB** | Catálogo, Mi lista, actividad de visualización y administradores |

## Stack

- **Backend:** Java 21, Spring Boot 3.5 (Web, Data JPA, Data MongoDB, Security, Validation)
- **Frontend:** React + Vite, React Router, Axios
- **Bases de datos:** PostgreSQL 17 y MongoDB 8 en Docker

## Estructura

```
Cine/
├── backend/                 API REST en Spring Boot
│   └── src/main/java/com/cineconecta/
│       ├── cuentas/         RF-01 a RF-04
│       ├── perfiles/        RF-05 a RF-10
│       ├── suscripciones/   RF-11 a RF-13
│       ├── catalogo/        RF-14 a RF-23
│       ├── milista/         RF-24 a RF-26
│       ├── reproduccion/    RF-27 a RF-32
│       ├── admin/           RF-33 y RF-34
│       ├── informes/        RF-35 a RF-37
│       ├── security/        JWT y autorización
│       ├── common/          Errores y utilidades
│       └── config/          Configuración y /api/health
├── frontend/                Interfaz en React
├── db/
│   ├── postgres/            Scripts de creación y carga (se ejecutan en orden)
│   └── mongo/               Colecciones, validadores, índices y carga
├── docs/                    Justificación de decisiones y evidencias de optimización
├── scripts/                 Scripts de la demo (concurrencia y fallo)
├── docker-compose.yml
└── .env.example
```

## Requisitos

- Docker Desktop
- JDK 21
- Maven (o IntelliJ IDEA, que lo trae integrado)
- Node.js 20 o superior

## Cómo ejecutar

### 1. Variables de entorno

```bash
cp .env.example .env
```

Cambiar las contraseñas en `.env`. Ese archivo **no se sube** al repositorio.

### 2. Bases de datos

```bash
docker compose up -d
```

- PostgreSQL queda en `localhost:5433` y MongoDB en `localhost:27018` (puertos distintos a los por defecto para no chocar con instalaciones locales).
- Los scripts de `db/postgres` y `db/mongo` se ejecutan solos **solo la primera vez**. Para recrear las bases desde cero:

```bash
docker compose down -v
docker compose up -d
```

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

Queda en `http://localhost:8080`. Verificación: `http://localhost:8080/api/health` debe responder `{"postgres":"ok","mongo":"ok"}`.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda en `http://localhost:5173`. Las peticiones a `/api` se redirigen al backend.

## Conexión con clientes de base de datos

| Base | Cliente | Conexión |
|---|---|---|
| PostgreSQL | pgAdmin, DBeaver o psql | `localhost:5433`, base `cineconecta`, usuario y contraseña del `.env` |
| MongoDB | Compass o mongosh | `mongodb://<MONGO_ADMIN_USER>:<MONGO_ADMIN_PASSWORD>@localhost:27018/?authSource=admin` |

## Decisiones base

- Hibernate **no crea tablas** (`ddl-auto: validate`): el esquema, las restricciones, los triggers y las funciones se crean con los scripts de `db/postgres`.
- Los nombres de columnas se usan tal cual (`accountId`), sin conversión a `account_id`.
- La aplicación se conecta con usuarios de mínimo privilegio (`cineconecta_app`), no con el administrador de la base.
- No hay transacciones entre las dos bases; ningún flujo escribe en ambas a la vez (ver `docs/decisiones.md`).

## Usuarios de prueba

Los carga `db/postgres/06_seed.sql`. Todas las cuentas usan la contraseña **`Cine2026*`** (en la base solo se guarda su hash BCrypt).

| Cuenta | Situación | Escenario de la demo |
|---|---|---|
| ana@cineconecta.com | Estándar vigente, 2/2 perfiles (Tomás es infantil, edad máxima 7) | Rechazo por máximo de perfiles y por clasificación no permitida |
| bruno@cineconecta.com | Suscripción vencida | Rechazo por suscripción vencida |
| carla@cineconecta.com | Sin suscripción | Contratación, concurrencia y fallo antes del pago |
| diego@cineconecta.com | Premium vigente, 3 perfiles activos y 1 inactivo | Flujo normal |
| elena@cineconecta.com | Básico vigente | Flujo normal |
