#!/bin/bash
# Crea los usuarios de base de datos que usa la aplicación.
# Las contraseñas vienen de variables de entorno (archivo .env), no se escriben aquí.
# Los permisos (GRANT) de cada rol se definen en 05_grants.sql.
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE ROLE ${PG_APP_USER} LOGIN PASSWORD '${PG_APP_PASSWORD}';
  CREATE ROLE ${PG_REPORT_USER} LOGIN PASSWORD '${PG_REPORT_PASSWORD}';
EOSQL
