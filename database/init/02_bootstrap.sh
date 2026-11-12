#!/usr/bin/env bash
# =============================================================================
# Bootstrap de producción: crea el plantel base, el administrador y un usuario
# docente de pruebas a partir de variables de entorno.
#
# Lo ejecuta automáticamente la imagen oficial de PostgreSQL la primera vez que
# se inicializa un volumen vacío (docker-entrypoint-initdb.d).
#
# Variables requeridas:
#   ADMIN_EMAIL, ADMIN_PASSWORD
#   TEACHER_EMAIL, TEACHER_PASSWORD
# Opcional:
#   PLANTEL_NOMBRE (default: CUCII)
# =============================================================================
set -euo pipefail

: "${POSTGRES_USER:?POSTGRES_USER no está definido}"
: "${POSTGRES_DB:?POSTGRES_DB no está definido}"
: "${ADMIN_EMAIL:?ADMIN_EMAIL no está definido}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD no está definido}"
: "${TEACHER_EMAIL:?TEACHER_EMAIL no está definido}"
: "${TEACHER_PASSWORD:?TEACHER_PASSWORD no está definido}"

PLANTEL_NOMBRE="${PLANTEL_NOMBRE:-CUCII}"

if [ "${#ADMIN_PASSWORD}" -lt 12 ]; then
  echo "ERROR: ADMIN_PASSWORD debe tener al menos 12 caracteres." >&2
  exit 1
fi
if [ "${#TEACHER_PASSWORD}" -lt 12 ]; then
  echo "ERROR: TEACHER_PASSWORD debe tener al menos 12 caracteres." >&2
  exit 1
fi
case "$ADMIN_PASSWORD" in
  CAMBIAR_*) echo "ERROR: reemplaza ADMIN_PASSWORD en .env.prod." >&2; exit 1 ;;
esac
case "$TEACHER_PASSWORD" in
  CAMBIAR_*) echo "ERROR: reemplaza TEACHER_PASSWORD en .env.prod." >&2; exit 1 ;;
esac

echo "==> Creando catálogos base, administrador y docente..."

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v admin_email="$ADMIN_EMAIL" \
  -v admin_password="$ADMIN_PASSWORD" \
  -v teacher_email="$TEACHER_EMAIL" \
  -v teacher_password="$TEACHER_PASSWORD" \
  -v plantel_nombre="$PLANTEL_NOMBRE" <<'EOSQL'
-- pgcrypto aporta crypt()/gen_salt() para generar bcrypt compatible con
-- BCryptPasswordEncoder de Spring Security.
create extension if not exists pgcrypto;

-- Plantel base. El administrador y el docente necesitan un plantel porque
-- usuarios.plantel_id es NOT NULL. Se puede editar después desde la app.
insert into planteles (nombre_oficial, nombre_corto)
select :'plantel_nombre', :'plantel_nombre'
where not exists (select 1 from planteles);

insert into usuarios (nombre, apellido, email, password_hash, rol_id, plantel_id)
select
    'Administrador',
    'Sistema',
    :'admin_email',
    crypt(:'admin_password', gen_salt('bf', 10)),
    (select id from roles where nombre = 'admin'),
    (select id from planteles order by id limit 1)
on conflict (email) do nothing;

insert into usuarios (nombre, apellido, email, password_hash, rol_id, plantel_id)
select
    'Docente',
    'Prueba',
    :'teacher_email',
    crypt(:'teacher_password', gen_salt('bf', 10)),
    (select id from roles where nombre = 'docente'),
    (select id from planteles order by id limit 1)
on conflict (email) do nothing;
EOSQL

echo "==> Bootstrap completado."
