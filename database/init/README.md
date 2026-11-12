# Inicialización de la base de datos

## Producción

El servicio `db` de `docker-compose.prod.yaml` monta:

1. `database/db_structure.sql` → esquema + catálogos (`roles`, `estatus_alumnos`).
2. `database/init/02_bootstrap.sh` → crea el plantel base, el admin y el docente
   usando las variables `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `TEACHER_EMAIL`,
   `TEACHER_PASSWORD` y `PLANTEL_NOMBRE`.

PostgreSQL solo ejecuta estos archivos cuando el volumen está **vacío**.

## ¿Qué se considera catálogo?

- `roles` (admin, rector, docente, servicios_escolares, coordinador)
- `estatus_alumnos` (Invasión, Cursando, Egresado, Baja)

Los datos académicos reales (planes de estudio, materias, grupos, alumnos y
calificaciones) **no** se siembran: se capturan desde la aplicación.

## Datos demo

`database/demo/demo_data.sql` contiene 40 alumnos, 4 grupos, 8 docentes y
calificaciones de ejemplo. Es **solo para desarrollo** y asume una base limpia
donde se aplicó únicamente `db_structure.sql` (sin ejecutar el bootstrap).

Hitos pendientes (migraciones, CSV, roles, etc.):
[`../../docs/HITOS_PENDIENTES.md`](../../docs/HITOS_PENDIENTES.md).
