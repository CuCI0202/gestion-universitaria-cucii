# Despliegue — Gestión Universitaria CUCII

Arquitectura:

```
Cloudflare Pages                     VPS (Docker)
gestion.cucii.mx  ───────────────▶  caddy (80/443, Let's Encrypt)
                                     └── api:8080 (Spring Boot)
                                          └── db:5432 (PostgreSQL 18)
```

- La SPA se publica en **Cloudflare Pages**.
- El VPS solo aloja la **base de datos**, el **API** y **Caddy**.
- El API se expone en `https://api.gestion.cucii.mx`.
- La autenticación viaja en el header `Authorization: Bearer <token>`.

---

## 0. Requisitos

| Recurso | Detalle |
|---------|---------|
| VPS | Ubuntu 22.04/24.04, 2 GB RAM mínimo, Docker + Docker Compose |
| DNS | `gestion.cucii.mx` → Cloudflare Pages; `api.gestion.cucii.mx` → IP del VPS |
| Puertos | 22 (SSH), 80 y 443 abiertos; **5432 y 8080 no se publican** |
| Cloudflare | Cuenta con Pages y el dominio `cucii.mx` |

> **Importante:** deja el registro DNS de `api.gestion.cucii.mx` como **DNS only**
> (nube gris) para que Caddy pueda emitir el certificado con Let's Encrypt.

---

## 1. Preparar el VPS

```bash
# Como root o usuario con sudo
apt update && apt upgrade -y
curl -fsSL https://get.docker.com | sh
usermod -aG docker "$USER"   # vuelve a iniciar sesión después
docker compose version
```

Clona el repositorio en el VPS:

```bash
git clone https://github.com/CuCI0202/gestion-universitaria-cucii.git
cd gestion-universitaria-cucii
git checkout main
```

---

## 2. Configurar variables de producción

```bash
cp deploy/.env.prod.example .env.prod
chmod 600 .env.prod
```

Genera secretos:

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 24   # POSTGRES_PASSWORD
openssl rand -base64 24   # ADMIN_PASSWORD
openssl rand -base64 24   # TEACHER_PASSWORD
```

Edita `.env.prod` y reemplaza todos los `CAMBIAR_...`. Las contraseñas de
bootstrap deben tener **mínimo 12 caracteres**.

> `.env.prod` está en `.gitignore`. Nunca lo subas al repositorio ni lo pegues en
> un chat.

---

## 3. Primer arranque

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

Verifica el arranque:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml ps
docker compose --env-file .env.prod -f docker-compose.prod.yaml logs -f db
docker compose --env-file .env.prod -f docker-compose.prod.yaml logs -f api
```

El primer arranque de `db` ejecuta:

1. `database/db_structure.sql` → esquema + catálogos (`roles`, `estatus_alumnos`).
2. `database/init/02_bootstrap.sh` → plantel base + admin + docente.

Ambos scripts **solo corren con el volumen vacío**. Si necesitas reinicializar en
un entorno de pruebas:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml down -v
```

> `down -v` borra la base de datos. No lo uses en producción con datos reales.

---

## 4. Verificar

```bash
# Health del API (debe responder {"status":"UP"})
curl -fsS https://api.gestion.cucii.mx/actuator/health

# Login del admin
curl -fsS -X POST https://api.gestion.cucii.mx/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@cucii.edu.mx","password":"<ADMIN_PASSWORD>"}'
```

Si el certificado aún no está listo, revisa:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml logs -f caddy
```

Causas típicas: DNS aún no propaga, el puerto 80/443 está ocupado por otro
servicio, o el registro de `api` está proxied (nube naranja) en Cloudflare.

---

## 5. Cloudflare Pages (frontend)

1. En Cloudflare Pages: **Create a project** → conecta el repositorio de GitHub.
2. Configuración de build:
   - **Framework preset:** None / Angular
   - **Root directory:** `view`
   - **Build command:** `pnpm install && pnpm build`
   - **Build output directory:** `dist/calificaciones/browser`
   - **Variable de entorno:** `NODE_VERSION=22`
3. Agrega el dominio personalizado `gestion.cucii.mx`.
4. Verifica que `view/public/_redirects` llegue al build (SPA fallback).

`view/src/environments/environment.prod.ts` ya apunta a
`https://api.gestion.cucii.mx`.

Si usas preview deployments de Pages, agrega cada origen exacto a
`CORS_ORIGINS` (separados por coma). Spring Security usa coincidencia exacta de
orígenes; los comodines de subdominio no están habilitados.

---

## 6. Backups

El script `scripts/backup.sh` genera un `pg_dump` en formato custom, valida el
archivo y aplica retención.

```bash
chmod +x scripts/backup.sh
./scripts/backup.sh
```

Cron diario a las 03:00:

```cron
0 3 * * * cd /opt/gestion-universitaria-cucii && ./scripts/backup.sh >> /var/log/gestion-backup.log 2>&1
```

Copia los respaldos fuera del VPS (otro disco, S3, Backblaze, etc.). Un respaldo
que solo vive en el mismo VPS no protege contra la pérdida del servidor.

Restaurar un respaldo:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yaml exec -T db \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
  < backups/gestion-AAAAMMDD-HHMMSS.dump
```

---

## 7. Operación diaria

```bash
# Estado
docker compose --env-file .env.prod -f docker-compose.prod.yaml ps

# Logs
docker compose --env-file .env.prod -f docker-compose.prod.yaml logs -f api

# Reiniciar API
docker compose --env-file .env.prod -f docker-compose.prod.yaml restart api

# Actualizar código
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

### Migraciones

Hoy el esquema se aplica **solo al crear el volumen**. Antes de la primera carga
de datos reales, cualquier cambio de esquema debe hacerse con un script SQL
versionado y aplicado manualmente, o migrando a Flyway. No borres el volumen de
producción para “actualizar”.

---

## 8. Checklist de seguridad

- [ ] `JWT_SECRET` distinto del valor de desarrollo y de al menos 32 caracteres.
- [ ] Contraseña de admin cambiada después del primer login.
- [ ] `POSTGRES_PASSWORD` fuerte y no reutilizada.
- [ ] `.env.prod` con permisos `600` y fuera de git.
- [ ] Puertos 5432 y 8080 no publicados al exterior.
- [ ] Firewall del VPS: solo 22, 80 y 443.
- [ ] Backups diarios + copia fuera del VPS + prueba de restore.
- [ ] `CORS_ORIGINS` solo con los dominios reales.
- [ ] Documentos de alumno (Google Drive) quedan fuera de esta fase.
