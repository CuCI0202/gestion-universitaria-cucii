# Plan de ejecución hacia Beta v0.1 — Gestión Universitaria CUCII

> Documento de continuidad. Si la sesión se interrumpe, retomar desde aquí.
> Estado: **Fase 0 completada**. En curso **1.1b** (frontend server-side search/filters). Batch CSV movido a Fase 3 (3.4/3.5).
> Última actualización: 2026-09-30

---

## 0. Cambio de identidad (rename)

El proyecto deja de llamarse "Calificaciones CUCII" y se renombra a
**Gestión Universitaria CUCII** (alcance más amplio que solo calificaciones).

| Elemento | Antes | Ahora |
|----------|-------|-------|
| Repo git / GitHub | `calificaciones.cucii.mx` | **`gestion-universitaria-cucii`** |
| Dominio | `calificaciones.cucii.mx` | **`gestion.cucii.mx`** |

Tareas asociadas (incluir en Fase 0 / Fase 2):
- [x] Renombrar el repositorio en GitHub y el remoto local (`git remote set-url`).
- [ ] Actualizar el dominio en: CORS (`SecurityConfig` / env `CORS_ORIGINS`), `environment.prod.ts`, config de HTTPS/Caddy, README.
- [ ] Actualizar títulos/descripciones de README raíz, `server/README.md`, `view/README.md` y `pom.xml`/`package.json` si aplica.
- [ ] Renombrar contenedor Docker y DB si se desea coherencia (hoy `cucii-ss-platform` / `ss-platform`).
- [ ] Verificar que no queden referencias a `calificaciones.cucii.mx` en docs ni config.

---

## 1. Contexto del proyecto

Monorepo **`gestion-universitaria-cucii`** (dominio `gestion.cucii.mx`) con tres componentes:

| Componente | Stack | Ubicación |
|------------|-------|-----------|
| Base de datos | PostgreSQL 18 (Docker) | `database/` |
| Backend | Spring Boot 4.0.6 · Java 21 · JdbcTemplate · JWT (JJWT) | `server/` |
| Frontend | Angular 21 (zoneless) · Tailwind 4 · Signals | `view/` |

Convenciones: ver `server/AGENTS.md` y `view/AGENTS.md`.
Código en inglés, texto al usuario en español.

### Estado git actual
- Rama activa: `develop` (sincronizada con `origin/develop`).
- Ramas locales: `develop`, `main`, `feat/pagination`.
- Remoto: `https://github.com/CuCI0202/calificaciones.cucii.mx.git` (pendiente renombrar).
- Sin cambios pendientes; solo `PLAN_BETA.md` sin trackear (documento de continuidad).
- Últimos hitos: paginación (`4f78b98`, `a1f663a`) y filtros/orden server-side (`f49d849`).

---

## 2. Decisiones tomadas (respuestas del usuario)

| Tema | Decisión |
|------|----------|
| Despliegue | VPS propio con Docker |
| Repo git / GitHub | `gestion-universitaria-cucii` |
| Dominio / HTTPS | `gestion.cucii.mx` con HTTPS (Let's Encrypt) |
| Usuarios beta | **Solo rol admin** (matriz fina de roles se difiere a post-beta) |
| Carga de calificaciones | Ambos: CSV masivo + captura manual |
| Datos reales | No hay; se capturan desde cero en la beta |

---

## 3. Diagnóstico: faltantes y pendientes

### Bloqueantes para beta
- [ ] Roles sin implementar en backend: `server/.../config/SecurityConfig.java:44-52` exige `ADMIN` en todas las rutas (para beta admin-only esto **no** bloquea, se difiere).
- [~] Búsqueda client-side sobre solo la primera página (`view/src/app/core/services/students.service.ts:48`) → no escala con datos reales. **Backend ya soporta `?search=` y filtros (commit `f49d849`); falta que el frontend los consuma.**
- [ ] CSV de calificaciones prometido en README pero inexistente; solo captura manual.
- [ ] Config hardcodeada en `server/src/main/resources/application.properties` (password BD, JWT secret, expiración 100 días con `TODO`).
- [ ] Cookie `Secure` fija en `view/src/app/core/services/cookie-utils.ts` (rompe sobre HTTP; con HTTPS de dominio está OK).
- [ ] CORS fijado a `http://localhost:4200` (`SecurityConfig.java:63`).
- [ ] `apiUrl` fijo a `localhost:8080`; no existe `environment.prod.ts` ni `fileReplacements` en `angular.json`.

### Infraestructura (cero despliegue)
- [ ] Sin Dockerfiles (server, view), sin compose full-stack, sin nginx.
- [ ] Sin CI/CD ni scripts de deploy.
- [ ] `.gitignore` casi vacío (no ignora `target/`, `node_modules/`, `dist/`).
- [ ] Sin migraciones versionadas: `init.sql` solo corre en el primer boot.
- [ ] Sin backups de BD ni manejo de ciclos escolares.

### Calidad / documentación
- [ ] Backend: solo `PlatformApplicationTests.contextLoads`.
- [ ] Frontend: solo `app.spec.ts` por defecto; sin e2e.
- [ ] README dice Angular 20.3 pero `view/package.json` tiene 21.2.17.
- [ ] Filtro JWT no excluye `/auth/login` → login falla 403 si hay token expirado en el header.
- [ ] Falta validación: calificación debe verificar que el alumno pertenezca al grupo.

---

## 4. Plan de ejecución

### Fase 0 — Higiene base (habilitadores) — COMPLETA (rama `chore/fase-0-hardening`)
- [x] **0.1** `.gitignore` raíz ampliado (`.env`, `.env.*`, `!.env.example`, `.idea/`, `*.iml`, `.vscode/`, `*.log`). `server/.gitignore` y `view/.gitignore` ya cubrían `target/`, `node_modules/`, `dist/`, `.angular/`.
- [x] **0.2** Backend configurable por env vars (`DB_URL/DB_USER/DB_PASSWORD/JWT_SECRET/JWT_EXPIRATION/CORS_ORIGINS`) con defaults dev + `application-prod.properties` estricto (falla si faltan). JWT default 24h. `.env.example` en raíz.
- [x] **0.3** `view/src/environments/environment.prod.ts` (`https://gestion.cucii.mx/api`) + `fileReplacements` en `view/angular.json`. Verificado en build prod.
- [x] **0.4** Cookies `Secure` condicional a HTTPS (`view/src/app/core/services/cookie-utils.ts`).
- [x] **0.5** Fix `JwtAuthenticationFilter.shouldNotFilter("/auth/login")`.
- [x] **0.6** CORS desde `app.cors.allowed-origins` (env var, lista separada por comas).
- [x] **0.7** Renombrar repo a `gestion-universitaria-cucii` y actualizar remoto (`git remote set-url`).

> Nota Fase 2: nginx debe hacer `proxy_pass http://api:8080/;` para `/api/` (strip del prefijo), consistente con `environment.prod.ts`.

### Fase 1 — Funcionalidad mínima beta
- [x] **1.1a** Backend búsqueda/filtros/orden server-side en todos los GET paginados (commit `f49d849`). Rutas y params documentados en el historial del commit.
- [ ] **1.1b** Frontend: consumir `?search=`, filtros FK e `isActive` en Upload, Browse y pantallas de listado (hoy filtran client-side sobre la primera página).
- [ ] **1.4** Validación en captura: el alumno debe pertenecer al grupo al registrar calificación.
- [ ] **1.5** Catálogos completos donde la UI los use como select (evitar truncado por paginación).

### Fase 2 — Despliegue beta en VPS
- [ ] **2.1** `server/Dockerfile` (multi-stage: Maven → `eclipse-temurin:21-jre`).
- [ ] **2.2** `view/Dockerfile` (node build → `nginx:alpine`) + `nginx.conf` (SPA fallback, proxy `/api`, gzip).
- [ ] **2.3** `docker-compose.prod.yaml` raíz: `db` (volumen + healthcheck), `api`, `web`; `.env.example` documentado.
- [ ] **2.4** HTTPS con Caddy o certbot frente al stack (Let's Encrypt) para `gestion.cucii.mx`.
- [ ] **2.4.1** Apuntar DNS `gestion.cucii.mx` (A/AAAA) al VPS y emitir el certificado.
- [ ] **2.5** Migraciones: carpeta `database/migrations/` versionadas + script de apply.
- [ ] **2.6** Backups: cron diario `pg_dump` + `scripts/backup.sh` con retención N días.
- [ ] **2.7** Primer deploy + smoke test (login).

### Fase 3 — Carga de datos reales + congelar beta
- [ ] **3.1** Orden de captura: planteles → planes de estudio → materias → estatus → grupos → alumnos (manual/CSV) → asignaciones alumno-grupo → calificaciones (manual/CSV).
- [ ] **3.2** Smoke test end-to-end: login → browse → captura manual → captura CSV → CRUDs admin.
- [ ] **3.3** Tag `v0.1.0-beta` en git como punto estable.
- [ ] **3.4** Batch de calificaciones (movido de 1.2): `POST /calificaciones/batch` transaccional con reporte de errores por fila + `addMany` en `GradesService` + importación CSV en `/upload` (previsualización, validación CURP/rango, guardado).
- [ ] **3.5** (Opcional) Batch de alumnos (movido de 1.3): `POST /alumnos/batch` para carga inicial vía CSV.

### Fase 4 — Post-beta (mejora continua)
- [ ] Matriz de roles real (docente solo sus grupos/materias; coordinador; servicios escolares; rector).
- [ ] Ciclos escolares (hoy los grupos no tienen periodo) e historial por periodo.
- [ ] Kardex/boleta y export PDF/CSV.
- [ ] Tests backend (lógica + seguridad) y e2e frontend.
- [ ] CI/CD (build + test + deploy automático).
- [ ] Observabilidad básica (logs + `/actuator/health`).

---

## 5. Riesgos y notas

- **Unique `alumno+materia+grupo`**: suficiente para beta con un solo periodo; el historial por periodo requerirá migrar el esquema.
- **Sin `POST /auth/logout`** (logout client-side borrando cookies): aceptable con JWT stateless de 24h.
- **Datos desde cero**: definir primero los catálogos reales (planes y materias por cuatrimestre), ya que grupos y calificaciones dependen de ellos.
- **Beta admin-only**: no se toca la matriz de roles ni `adminGuard` en esta etapa.
- **Rename**: buscar y reemplazar todas las referencias a `calificaciones.cucii.mx` (docs, CORS, env, compose, Docker) por `gestion.cucii.mx` / `gestion-universitaria-cucii` antes del deploy.

---

## 6. Referencias rápidas

| Tema | Archivo |
|------|---------|
| Seguridad / roles | `server/src/main/java/mx/cucii/school/platform/config/SecurityConfig.java` |
| Config backend | `server/src/main/resources/application.properties` |
| Filtro JWT | `server/src/main/java/mx/cucii/school/platform/security/JwtAuthenticationFilter.java` |
| Servicio alumnos (search) | `server/src/main/java/mx/cucii/school/platform/service/AlumnoService.java` |
| Servicio calificaciones (batch) | `server/src/main/java/mx/cucii/school/platform/service/CalificacionService.java` |
| Env frontend | `view/src/environments/environment.ts` |
| Cookies | `view/src/app/core/services/cookie-utils.ts` |
| Búsqueda/upload | `view/src/app/features/upload/upload.ts` |
| Servicio alumnos FE | `view/src/app/core/services/students.service.ts` |
| Build Angular | `view/angular.json` |
| Esquema BD | `database/db_structure.sql` |
| Compose BD (dev) | `database/docker-compose.yaml` |

### Comandos de desarrollo
```bash
# BD
cd database && docker compose up -d

# Backend
cd server && ./mvnw spring-boot:run

# Frontend
cd view && pnpm install && pnpm start

# Renombrar remoto tras el cambio de repo
git remote set-url origin git@github.com:<org>/gestion-universitaria-cucii.git

# Credenciales de prueba
# admin@cucii.edu.mx / Test1234!
```
