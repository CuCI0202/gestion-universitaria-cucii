# Plan de ejecución hacia Beta v0.1 — Gestión Universitaria CUCII

> Documento de continuidad. Si la sesión se interrumpe, retomar desde aquí.
> Estado: **Fase 0 + Fase 1 completadas**. Hito A de despliegue (kit Docker/DB/Caddy/backups) implementado y validado localmente; pendiente el VPS y el DNS de `api`. Batch CSV movido a Fase 3 (3.4/3.5).
> Última actualización: 2026-11-12

---

## 0. Cambio de identidad (rename)

El proyecto deja de llamarse "Calificaciones CUCII" y se renombra a
**Gestión Universitaria CUCII** (alcance más amplio que solo calificaciones).

| Elemento | Antes | Ahora |
|----------|-------|-------|
| Repo git / GitHub | `calificaciones.cucii.mx` | **`gestion-universitaria-cucii`** |
| Dominio | `calificaciones.cucii.mx` | **`gestion.cucii.mx`** |

Tareas asociadas:
- [x] Renombrar el repositorio en GitHub y el remoto local (`git remote set-url`).
- [x] Actualizar el dominio en CORS (`CORS_ORIGINS`), `environment.prod.ts`, `deploy/Caddyfile`, docs y README.
- [x] Actualizar títulos/descripciones de README raíz, `server/README.md`, `view/README.md`, `server/AGENTS.md` y `view/AGENTS.md`.
- [x] Usar nombres de contenedores `gestion-cucii-*` en producción. La DB de desarrollo conserva `cucii-ss-platform`; no es bloqueante.
- [x] Revisar referencias a `calificaciones.cucii.mx` en docs/config. Las que quedan son históricas o de la carpeta local.

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
- Rama activa: `develop`, con el merge del kit de despliegue (`f77255f`).
- `main` está detrás de `develop`; se actualizará con un merge antes de desplegar.
- Remoto: `https://github.com/CuCI0202/gestion-universitaria-cucii.git`.
- Árbol limpio; el trabajo de despliegue está en `develop`.
- Últimos hitos: kit de despliegue Docker (`493abcd`) y merge a `develop` (`f77255f`).

---

## 2. Decisiones tomadas (respuestas del usuario)

| Tema | Decisión |
|------|----------|
| Despliegue | VPS propio con Docker; **VPS pendiente de aprovisionar** |
| Repo git / GitHub | `gestion-universitaria-cucii` |
| Dominio / HTTPS | `gestion.cucii.mx` + `api.gestion.cucii.mx` con Caddy (Let's Encrypt) como servicio Docker |
| Usuarios beta | **admin + docente**. El docente puede consultar y registrar/actualizar calificaciones; los CRUDs y el borrado siguen siendo admin |
| Carga de calificaciones | **Ambos** (confirmado): CSV masivo + captura manual |
| Datos reales | Base limpia: catálogos (`roles`, `estatus_alumnos`), 1 plantel, admin y docente. Sin datos académicos |
| Google Drive | **Excluido por ahora**; corresponde al hito HJ |

---

## 3. Estado actual y pendientes

> El detalle completo, criterios de aceptación y orden recomendado viven en
> [`docs/HITOS_PENDIENTES.md`](docs/HITOS_PENDIENTES.md).

### Resuelto
- [x] Roles beta: `admin` y `docente` operativos; `rector`, `servicios_escolares` y `coordinador` quedan para la matriz fina (HG).
- [x] Búsqueda, filtros y orden server-side en todos los listados.
- [x] Configuración por variables de entorno + perfil `prod` estricto.
- [x] Cookie `Secure` condicional a HTTPS y CORS configurable.
- [x] `environment.prod.ts` + `fileReplacements`; build de producción verificado.
- [x] Filtro JWT excluye `/auth/login`; tokens inválidos responden 401.
- [x] Validación alumno↔grupo en service + FK compuesta.
- [x] Dockerfile del API, compose de producción, Caddy Docker, backups y guía de despliegue.
- [x] Base limpia + bootstrap de admin/docente; datos demo separados.
- [x] README y AGENTS alineados con Angular 21.2 y el estado actual.

### Pendiente
- [ ] **HB** Despliegue real en VPS + Cloudflare Pages + smoke test.
- [ ] **HE** Migraciones versionadas, backup externo y restore probado.
- [ ] **HC** Editar/eliminar calificaciones desde UI y reactivar asignaciones/calificaciones archivadas.
- [ ] **HD** CSV masivo de alumnos y calificaciones (ambos requeridos).
- [ ] **HF** Endurecimiento, observabilidad y CI/CD.
- [ ] **HG** Matriz de roles y alcance por docente/grupo/materia.
- [ ] **HH** Ciclos escolares e historial por periodo.
- [ ] **HI** Kardex/boleta y export PDF/CSV.
- [ ] **HJ** Google Drive + iconos.
- [ ] **HK** Tests backend/frontend/e2e.
- [ ] **HL** Cierre beta y tag `v0.1.0-beta`.

---

## 4. Plan de ejecución

### Fase 0 — Higiene base (habilitadores) — COMPLETA (rama `chore/fase-0-hardening`)
- [x] **0.1** `.gitignore` raíz ampliado (`.env`, `.env.*`, `!.env.example`, `.idea/`, `*.iml`, `.vscode/`, `*.log`). `server/.gitignore` y `view/.gitignore` ya cubrían `target/`, `node_modules/`, `dist/`, `.angular/`.
- [x] **0.2** Backend configurable por env vars (`DB_URL/DB_USER/DB_PASSWORD/JWT_SECRET/JWT_EXPIRATION/CORS_ORIGINS`) con defaults dev + `application-prod.properties` estricto (falla si faltan). JWT default 24h. `.env.example` en raíz.
- [x] **0.3** `view/src/environments/environment.prod.ts` (`https://api.gestion.cucii.mx`) + `fileReplacements` en `view/angular.json`. Verificado en build prod.
- [x] **0.4** Cookies `Secure` condicional a HTTPS (`view/src/app/core/services/cookie-utils.ts`).
- [x] **0.5** Fix `JwtAuthenticationFilter.shouldNotFilter("/auth/login")`.
- [x] **0.6** CORS desde `app.cors.allowed-origins` (env var, lista separada por comas).
- [x] **0.7** Renombrar repo a `gestion-universitaria-cucii` y actualizar remoto (`git remote set-url`).

> Nota de arquitectura de despliegue: la **SPA se publica en Cloudflare Pages** y el **VPS solo aloja la DB y el API**. El API se expone en `https://api.gestion.cucii.mx` (Caddy + Let's Encrypt) y el frontend consume ese origen vía `environment.prod.ts`. Ya **no** se usa nginx ni `view/Dockerfile`. La auth viaja en el header `Authorization`, así que no hay cookies cross-site.

### Fase 1 — Funcionalidad mínima beta
- [x] **1.1a** Backend búsqueda/filtros/orden server-side en todos los GET paginados (commit `f49d849`). Rutas y params documentados en el historial del commit.
- [x] **1.1b** Frontend server-side search/filtros: helper `core/services/http-params.ts` (`buildParams`/`QueryFilters`); `loadPage(page, size?, filters?)` en todos los servicios con filtros persistidos; Upload con `searchStudents()` + `getByStudent`/`getByIds`; Browse con `?curp=`/`?search=`/`isActive`; pantallas Students/Groups/Users/Campuses/Programs buscan en servidor; corregido `GradesService.getByStudent` a respuesta paginada.
- [x] **1.1c** GroupStudents server-side: nuevos endpoints `GET /grupos/{id}/alumnos` y `GET /grupos/{id}/alumnos-disponibles` (ambos paginados con `?search=`); frontend usa `getStudentsByGroup`/`getAvailableStudents` y `removeByGroupAndStudent`. Validado contra la BD.
- [x] **1.1d** Profesores server-side: `GET /profesores-grupos` ahora devuelve `ProfesorGrupoDetalleResponse` con nombres de profesor/grupo/materia y acepta `?search=` (ILIKE sobre los tres). Frontend sin cruces client-side; selects con `size=100`. Validado contra la BD.
- [x] **1.4** Validación alumno↔grupo al registrar/actualizar calificación: `CalificacionService.validateEnrollment` (fila activa en `alumnos_grupos`) + FK compuesta `fk_calificaciones_alumno_grupo` en `database/db_structure.sql`. Validado (400 con mensaje claro; constraint en BD).
- [x] **1.5** Catálogos completos en selects (opción 1): `ProgramsService.getCatalog()` y `CampusesService.getCatalog()` (`size=100`); Groups/Users/Subjects cargan catálogos en señales locales. Profesores ya usaba `size=100`. Pendiente post-beta: migrar a typeahead si un catálogo supera 100.
- [x] **1.6** Gestión global de errores: `NotificationService` + `ToastComponent` (global en App) + `errorInterceptor` que muestra el mensaje del backend en cualquier error (salvo 401); opt-out `SKIP_ERROR_NOTIFICATION`. Backend: mensajes diferenciados para duplicados de alumno archivado + `POST /alumnos/{id}/restore` (botón "Restaurar" y toasts en la pantalla de Alumnos). Base aplicable a toda la plataforma; 1.7 la extiende a los demás CRUDs.
- [x] **1.7** Extender a **Grupos, Materias, Planes de estudio, Planteles y Usuarios**: UX de archivados (toggle "Mostrar archivados" + badge "Archivado — Eliminado" + botón "Restaurar" con endpoint `POST /{recurso}/{id}/restore`) y toasts de éxito en crear/editar/archivar. La **visibilidad de errores ya es global** vía `errorInterceptor`. Backend: `restore` en los 5 servicios/repositorios/controllers; el restore de plan de estudio **cascada** a sus materias. Frontend: `isActive` agregado a modelos `Group/Campus/Program/Subject`, filtro por defecto `{isActive:true}` y `restore()` en los servicios; la pantalla Materias usa `GET /materias?planEstudioId=&isActive=` para poder listar archivadas.

### Fase 2 — Despliegue beta (SPA en Cloudflare Pages · DB + API en VPS)
- [x] **2.1** `server/Dockerfile` (multi-stage: Maven → `eclipse-temurin:21-jre-alpine`), usuario no-root y healthcheck.
- [x] **2.2** Frontend preparado para **Cloudflare Pages**: root `view`, build `pnpm install && pnpm build`, output `dist/calificaciones/browser`, `view/public/_redirects` (`/* /index.html 200`). El proyecto de Pages se crea en 2.7. Sin `view/Dockerfile` ni nginx.
- [x] **2.3** `docker-compose.prod.yaml` raíz: `db` (volumen + healthcheck, **sin publicar 5432**), `api` y `caddy`; `deploy/.env.prod.example` documentado. Sin servicio `web`.
- [x] **2.4** HTTPS con **Caddy como servicio Docker** (`caddy:2-alpine`) y `deploy/Caddyfile` para `api.gestion.cucii.mx`.
- [ ] **2.4.1** DNS: `api.gestion.cucii.mx` (A/AAAA) → VPS (Caddy emite cert); `gestion.cucii.mx` → Cloudflare Pages. **Pendiente: VPS.**
- [x] **2.4.2** `CORS_ORIGINS=https://gestion.cucii.mx` en `.env.prod` (y preview domains de Pages si aplica).
- [ ] **2.5** Migraciones: carpeta `database/migrations/` versionadas + script de apply. La init limpia ya está, pero falta el mecanismo incremental (hito HE).
- [~] **2.6** Backups: `scripts/backup.sh` con `pg_dump`, validación y retención; cron documentado. Falta copia externa y restore probado (hito HE).
- [ ] **2.7** Primer deploy + smoke test en VPS; incluye crear el proyecto de Cloudflare Pages. Validado localmente con Docker: login admin/docente, CRUDs y calificación (hito HB).
- [x] **2.8** Base limpia: `db_structure.sql` (esquema + catálogos) + `database/init/02_bootstrap.sh` (plantel, admin, docente). Datos demo movidos a `database/demo/demo_data.sql`.

### Fase 3 — Carga de datos reales + congelar beta
- [ ] **3.1** Orden de captura: planteles → planes de estudio → materias → estatus → grupos → alumnos (manual/CSV) → asignaciones alumno-grupo → calificaciones (manual/CSV).
- [ ] **3.2** Smoke test end-to-end: login → browse → captura manual → captura CSV → CRUDs admin.
- [ ] **3.3** Tag `v0.1.0-beta` en git como punto estable (hito HL).
- [ ] **3.4** Batch de calificaciones: `POST /calificaciones/batch` con reporte de errores por fila + `addMany` en `GradesService` + importación CSV en `/upload` (previsualización, validación CURP/rango, guardado) (hito HD).
- [ ] **3.5** Batch de alumnos: `POST /alumnos/batch` para carga inicial vía CSV. **Requerido**, ya no opcional (hito HD).

### Fase 4 — Post-beta (mejora continua)
- [ ] Matriz de roles real (docente solo sus grupos/materias; coordinador; servicios escolares; rector) — hito HG.
- [ ] Ciclos escolares (hoy los grupos no tienen periodo) e historial por periodo — hito HH.
- [ ] Kardex/boleta y export PDF/CSV — hito HI.
- [ ] Tests backend (lógica + seguridad) y e2e frontend — hito HK.
- [ ] CI/CD (build + test + deploy automático) — hito HF.
- [ ] Observabilidad básica (logs + `/actuator/health`) — hito HF.
- [ ] Google Drive + iconos — hito HJ (`PLAN_DRIVE_DOCUMENTACION.md`).

---

## 5. Riesgos y notas

- **Unique `alumno+materia+grupo`**: suficiente para beta con un solo periodo; el historial por periodo requerirá migrar el esquema (hito HH).
- **Soft-delete + UNIQUE**: quitar y volver a agregar alumnos/calificaciones puede bloquearse porque la fila inactiva conserva el `UNIQUE`. Se corrige en HC.
- **Sin `POST /auth/logout`** (logout client-side borrando cookies): aceptable con JWT stateless de 24h.
- **Datos desde cero**: definir primero los catálogos reales (planes y materias por cuatrimestre), ya que grupos y calificaciones dependen de ellos.
- **Beta admin + docente**: el docente no está restringido a sus grupos/materias; la matriz fina es HG.
- **Rename**: completado en docs/config; quedan referencias históricas en este plan y en la ruta local del repo.

---

## 6. Referencias rápidas

| Tema | Archivo |
|------|---------|
| Seguridad / roles | `server/src/main/java/mx/cucii/school/platform/config/SecurityConfig.java` |
| Config backend | `server/src/main/resources/application.properties` |
| Filtro JWT | `server/src/main/java/mx/cucii/school/platform/security/JwtAuthenticationFilter.java` |
| Servicio alumnos (search) | `server/src/main/java/mx/cucii/school/platform/service/AlumnoService.java` |
| Servicio calificaciones | `server/src/main/java/mx/cucii/school/platform/service/CalificacionService.java` |
| Env frontend | `view/src/environments/environment.ts` |
| Cookies | `view/src/app/core/services/cookie-utils.ts` |
| Búsqueda/upload | `view/src/app/features/upload/upload.ts` |
| Servicio alumnos FE | `view/src/app/core/services/students.service.ts` |
| Build Angular | `view/angular.json` |
| Esquema BD | `database/db_structure.sql` |
| Bootstrap BD | `database/init/02_bootstrap.sh` |
| Compose BD (dev) | `database/docker-compose.yaml` |
| Compose prod | `docker-compose.prod.yaml` |
| Caddy | `deploy/Caddyfile` |
| Guía de despliegue | `docs/DEPLOY.md` |
| Hitos pendientes | `docs/HITOS_PENDIENTES.md` |

### Comandos de desarrollo
```bash
# BD (volumen limpio: esquema + admin/docente)
cd database && docker compose up -d

# Backend
cd server && ./mvnw spring-boot:run

# Frontend
cd view && pnpm install && pnpm start

# Credenciales de desarrollo
# admin@cucii.edu.mx   / Admin1234!Dev
# docente@cucii.edu.mx / Docente1234!Dev
```

### Despliegue
```bash
cp deploy/.env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```
