#!/usr/bin/env bash
# =============================================================================
# Respaldo diario de la base de datos PostgreSQL.
#
# Uso:
#   ./scripts/backup.sh
#
# Variables opcionales:
#   ENV_FILE        default: .env.prod (raíz del repo)
#   COMPOSE_FILE    default: docker-compose.prod.yaml
#   BACKUP_DIR      default: ./backups
#   RETENTION_DAYS  default: 14
#
# Programar con cron (ejemplo: todos los días a las 03:00):
#   0 3 * * * cd /opt/gestion-universitaria-cucii && ./scripts/backup.sh >> /var/log/gestion-backup.log 2>&1
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yaml}"
BACKUP_DIR="${BACKUP_DIR:-$REPO_ROOT/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: no existe $ENV_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
target="$BACKUP_DIR/gestion-$timestamp.dump"

echo "==> Respaldando base de datos en $target"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T db \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$target"

if [ ! -s "$target" ]; then
  echo "ERROR: el respaldo quedó vacío: $target" >&2
  rm -f "$target"
  exit 1
fi

# Verificación rápida del formato custom de pg_dump.
if ! docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T db \
  pg_restore --list < "$target" > /dev/null; then
  echo "ERROR: el respaldo no es un dump válido: $target" >&2
  exit 1
fi

echo "==> Respaldo OK ($(du -h "$target" | cut -f1))"

echo "==> Eliminando respaldos con más de $RETENTION_DAYS días"
find "$BACKUP_DIR" -type f -name 'gestion-*.dump' -mtime +"$RETENTION_DAYS" -delete

echo "==> Listo."
