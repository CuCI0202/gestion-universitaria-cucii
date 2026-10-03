# Plan: Documentación de alumnos en Google Drive

> Documento de planificación. Al aprobarse, se ejecuta fase por fase.
> Última actualización: 2026-10-03
> Convenciones: `server/AGENTS.md` y `view/AGENTS.md`. Código en inglés, texto al usuario en español.

---

## 1. Contexto y objetivo

La documentación física de los alumnos (actas, CURP, comprobantes, kardex...) se
gestiona hoy **manualmente** en Google Drive (cuenta **Gmail personal**, carpeta
**compartida** con el equipo de servicios escolares).

**Objetivo:** permitir subir los documentos desde esta plataforma para que se
almacenen automáticamente en Google Drive, manteniéndolos **accesibles para
humanos desde Drive** como hasta ahora. Adicionalmente, se sustituyen los textos
de los botones de acción por **iconos** en todo el sistema.

## 2. Decisiones tomadas

| Tema | Decisión |
|------|----------|
| Integración Google | **OAuth 2.0 con refresh token** de la cuenta Gmail dueña de la carpeta |
| Scope Google | `https://www.googleapis.com/auth/drive.file` (no sensible) |
| Visibilidad humana | Los archivos quedan en la cuenta Gmail real, en la carpeta compartida existente |
| Tipos de documento | **Catálogo fijo** en BD (`tipos_documento`) |
| Múltiples por tipo | **Sí** — varios archivos del mismo tipo por alumno, borrado manual |
| Roles con acceso | **admin + servicios_escolares**: lectura de alumnos + gestión de documentos. CRUD de alumnos sigue siendo solo admin (ver §11) |
| Iconos | **Todo el sistema** — SVGs inline Heroicons encapsulados en componente `app-icon` (sin dependencias nuevas) |
| Sincronización | **Unidireccional**: la plataforma es el camino de escritura. Archivos agregados manualmente en Drive NO aparecen en la app |

## 3. Arquitectura general

```
Navegador (Angular)                  Servidor (Spring Boot)               Google Drive
──────────────────                  ────────────────────────              ────────────
Modal documentos                     GoogleDriveService
  │  multipart (archivo+tipo)          │  Credential(refresh_token)
  ├─────────── POST /alumnos/{id}/documentos ──────────▶ upload → archivo
  │                                   │  getOrCreateFolder "Documentacion/{CURP}/"
  │◀────────── DocumentoAlumnoResponse ────────────────┘
  │                                   │  insert BD (compensa borrando en Drive si falla)
  │  DELETE /documentos/{id} ─────────▶ soft-delete BD + delete en Drive
  │  GET /alumnos/{id}/documentos ────▶ lista desde BD (drive_web_link para abrir en Drive)
```

- Las credenciales de Google **nunca** van al navegador.
- La BD guarda `drive_file_id` y `drive_web_link`; la UI lista desde BD, no desde la API de Drive.

## 4. Cambios en base de datos (`database/db_structure.sql`)

```sql
-- Catálogo de tipos de documento
create table tipos_documento (
    id          int generated always as identity primary key,
    nombre      varchar(80)  not null unique,
    descripcion varchar(200),
    created_at  timestamptz  not null default now(),
    is_active   boolean      not null default true
);

-- Documentos del alumno (metadatos + referencia a Drive)
create table documentos_alumno (
    id                int generated always as identity primary key,
    alumno_id         int           not null references alumnos (id) on update cascade on delete restrict,
    tipo_documento_id int           not null references tipos_documento (id) on update cascade on delete restrict,
    nombre_archivo    varchar(255)  not null,
    mime_type         varchar(100)  not null,
    size_bytes        bigint        not null,
    drive_file_id     varchar(100)  not null unique,
    drive_web_link    text          not null,
    subido_por        int           references usuarios (id) on delete set null,
    created_at        timestamptz   not null default now(),
    is_active         boolean       not null default true
);

-- Carpeta de Drive por alumno (se crea lazy en el primer upload)
alter table alumnos add column documentos_folder_id varchar(100);
```

Seed de `tipos_documento` (sugerido, editable): Acta de nacimiento, CURP,
Comprobante de domicilio, Certificado de bachillerato, Kardex, Identificación
oficial, Fotografías, Otro.

**Migración:** `db_structure.sql` solo se ejecuta en el primer `docker compose up`.
Para la BD de desarrollo existente, ejecutar los `CREATE TABLE`/`ALTER TABLE`
manualmente (o recrear el volumen). En producción se aplican los mismos DDL.

## 5. Backend (Spring Boot)

### 5.1 Dependencias (`server/pom.xml`)
- `com.google.apis:google-api-services-drive` (v3)
- `com.google.oauth-client:google-oauth-client`
- `com.google.http-client:google-http-client-jackson2`

### 5.2 Configuración (`application.properties` / `application-prod.properties`)
```properties
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB

google.drive.client-id=${GOOGLE_CLIENT_ID:}
google.drive.client-secret=${GOOGLE_CLIENT_SECRET:}
google.drive.refresh-token=${GOOGLE_REFRESH_TOKEN:}
google.drive.root-folder-id=${GOOGLE_DRIVE_ROOT_FOLDER_ID:}
```
- En `application-prod.properties` van **obligatorias sin default** (`${GOOGLE_CLIENT_ID}`, etc.).
- `.env.example` (raíz) se amplía con las 4 variables.

### 5.3 Clases nuevas (patrón Controller → Service → Repository)

| Capa | Clase | Responsabilidad |
|------|-------|-----------------|
| model | `TipoDocumento`, `DocumentoAlumno` | Records con `@Table/@Id/@Column` (documentación) |
| repository | `TipoDocumentoJdbcRepository`, `DocumentoAlumnoJdbcRepository` | JdbcTemplate + RowMapper, SQL explícito (`findAll`, `findById`, `findByAlumnoId`, `save`, `softDeleteById`) |
| service | `GoogleDriveService` | Cliente Drive desde refresh token; `uploadFile(alumno, multipart) → (fileId, webViewLink)`, `deleteFile(fileId)`, `getOrCreateAlumnoFolder(curp) → folderId` (busca `Documentacion/{CURP}/` bajo la raíz; la crea si no existe y actualiza `alumnos.documentos_folder_id`) |
| service | `DocumentoAlumnoService` | Orquestación: listar, subir (Drive → BD → compensación), eliminar (Drive + soft-delete BD) |
| controller | `DocumentoAlumnoController` | Solo HTTP |
| dto | `TipoDocumentoResponse`, `DocumentoAlumnoResponse` | Nunca exponer el model |

### 5.4 Endpoints nuevos

| Método | Ruta | Descripción | Acceso |
|--------|------|-------------|--------|
| GET | `/tipos-documento` | Catálogo (paginado, `size=100` desde el FE) | ADMIN, SERVICIOS_ESCOLARES |
| GET | `/alumnos/{id}/documentos` | Docs activos del alumno (sin paginación) | ADMIN, SERVICIOS_ESCOLARES |
| POST | `/alumnos/{id}/documentos` | `multipart`: `file` + `tipoDocumentoId` → 201 | ADMIN, SERVICIOS_ESCOLARES |
| DELETE | `/documentos/{id}` | Borra de Drive + soft-delete → 204 | ADMIN, SERVICIOS_ESCOLARES |

### 5.5 Validaciones en upload
- Alumno existe y está activo; `tipoDocumentoId` existe.
- Mime permitidos: `application/pdf`, `image/jpeg`, `image/png`. Máx 20 MB.
- Nombre en Drive: `{CURP}_{TIPO}_{timestamp}.{ext}` (legible para humanos, sin colisiones).
- **Compensación:** si falla el `INSERT` tras subir a Drive → `deleteFile()` y error 500/400 limpio.
- Errores de Google (token revocado, cuota, red) → 502 con mensaje amigable en español vía `GlobalExceptionHandler`.

### 5.6 Seguridad (`SecurityConfig`)
```java
// ANTES de los matchers existentes (first-match-wins):
.requestMatchers(HttpMethod.GET, "/alumnos/**").hasAnyRole("ADMIN", "SERVICIOS_ESCOLARES")
.requestMatchers("/tipos-documento/**", "/documentos/**").hasAnyRole("ADMIN", "SERVICIOS_ESCOLARES")
// el resto de /alumnos/** (POST/PUT/DELETE) sigue hasRole("ADMIN")
```
Nota: el rol en DB es `servicios_escolares` → `ROLE_SERVICIOS_ESCOLARES`.

### 5.7 Tests
- `DocumentoAlumnoServiceTest`: subida OK, compensación al fallar BD, borrado, validaciones.
- `GoogleDriveServiceTest` con cliente Drive mockeado (estructura de carpetas, idempotencia de `getOrCreateAlumnoFolder`).

## 6. Frontend (Angular)

### 6.1 Componente `app-icon` (nuevo: `shared/components/icon/`)
- `inject` + signals; input `name: IconName` y `size` opcional; diccionario de paths SVG **inline Heroicons outline** (mismo estilo que el sidebar actual) + `title`/tooltip.
- Iconos necesarios: `pencil`, `trash`, `arrow-uturn-left`, `folder`, `document`, `document-plus`, `plus`, `eye`, `eye-slash`, `chevron-*`, `external-link`, `x-mark`, `cloud-arrow-up`, `check`, `exclamation-triangle`.

### 6.2 Iconos en todo el sistema (reemplazar textos)
- `students.html`: Editar→lápiz, Eliminar→bote, Restaurar→flecha-curva, + nuevo botón **carpeta** (documentación).
- Resto de tablas CRUD: `users`, `groups`, `programs`, `subjects`, `campuses`, `profesores` y botones tipo "Asignar"/"Quitar" en `group-students`.
- Mantener color semántico actual (primary/red) y `title` en cada botón.

### 6.3 Modal de documentación (nuevo: `features/students/student-documents-modal/`)
- Inputs: `alumno: Student`; emite evento de cierre.
- Sección lista: icono por tipo, `nombreArchivo`, fecha, quien subió, acciones "Abrir en Drive" (`driveWebLink` en pestaña nueva) y "Eliminar" (con `ConfirmService`).
- Sección subir: file input (arrastrar y soltar opcional) + select de tipo (`documentsService.getTiposDocumento()` con `size=100`) + barra de progreso (upload con `HttpRequest` + `reportProgress`).
- Modelos: `core/models/document.model.ts`; servicio: `core/services/documents.service.ts` (mismo patrón que los existentes; errores de upload con `SKIP_ERROR_NOTIFICATION` porque el modal muestra sus propios estados).

### 6.4 Rutas, guards y sidebar
- `app.routes.ts`: `/students` cambia de `adminGuard` a un guard que admita `admin|rector|servicios_escolares`.
- En `students.html`, los botones Editar/Eliminar/Agregar se muestran **solo si `auth.isAdmin()`** (servicios_escolares ve tabla + documentación en modo lectura).
- `sidebar.html`: mostrar "Alumnos" a `servicios_escolares`.

## 7. Tutorial: obtener el refresh token de Google Drive

> Para hacerlo UNA vez. Si el token se revoca o pierde, repetir pasos 5–7.

### Paso 1 — Crear proyecto en Google Cloud
1. Entrar a https://console.cloud.google.com/ con la cuenta Gmail dueña de la carpeta de documentación.
2. Crear proyecto nuevo, ej. `cucii-documentos` (nombre libre).

### Paso 2 — Habilitar la Google Drive API
1. Menú **APIs & Services → Library**.
2. Buscar "Google Drive API" → **Enable**.

### Paso 3 — Configurar pantalla de consentimiento OAuth
1. **APIs & Services → OAuth consent screen**.
2. User Type: **External** → Create.
3. App name: ej. `Documentación CUCII`; correo de soporte y de desarrollador: tu Gmail.
4. En **Scopes** agregar solo: `https://www.googleapis.com/auth/drive.file`
   (es un scope **no sensible**, no requiere verificación de Google).
5. En **Test users** agrega tu propio Gmail (solo si aún está en Testing).
6. **Publish app** → estado "In production".

> ⚠️ Importante: si la app queda en **Testing**, los refresh tokens **expiran a los 7 días**.
> En "In production" sin verificar, Google mostrará un aviso de "app no verificada"
> únicamente al momento de autorizar — es esperado y seguro para uso interno.

### Paso 4 — Crear credenciales OAuth (Client ID + Secret)
1. **APIs & Services → Credentials → Create credentials → OAuth client ID**.
2. Application type: **Web application**.
3. En "Authorized redirect URIs" agregar: `https://developers.google.com/oauthplayground`
   (usaremos OAuth Playground para obtener el token).
4. Guardar el **Client ID** y el **Client Secret** (se necesitan en el servidor).

### Paso 5 — Obtener el refresh token con OAuth Playground
1. Entrar a https://developers.google.com/oauthplayground
2. Icono de engranaje ⚙ (esquina superior derecha) → marcar
   **"Use your own OAuth credentials"** → pegar Client ID y Client Secret.
3. **Step 1** — panel izquierdo: buscar "Drive API v3" → expandir →
   marcar `https://www.googleapis.com/auth/drive.file`.
4. Click **Authorize APIs** → elegir **tu Gmail** (dueño de la carpeta).
   Si aparece el aviso de "Google hasn't verified this app" → **Advanced →
   Go to cucii-documentos (unsafe)** → continuar.
5. **Step 2** — click **Exchange authorization code for tokens**.
   Se generan `access_token` y `refresh_token`.
6. **Copiar el `refresh_token`** y guardarlo en un lugar seguro (gestor de
   secretos, `.env` del servidor — **nunca en el repositorio**).

### Paso 6 — Obtener el ID de la carpeta raíz en Drive
1. En Drive, abrir la carpeta donde vivirán los documentos (o crear
   "Documentación" en la cuenta Gmail).
2. Copiar el ID desde la URL: `https://drive.google.com/drive/folders/1AbCdEfGh123...`
   → lo que sigue de `folders/` es el `GOOGLE_DRIVE_ROOT_FOLDER_ID`.

### Paso 7 — Verificar que el token funciona (opcional)
```bash
curl -s -X POST https://oauth2.googleapis.com/token \
  -d "client_id=TU_CLIENT_ID" \
  -d "client_secret=TU_CLIENT_SECRET" \
  -d "refresh_token=TU_REFRESH_TOKEN" \
  -d "grant_type=refresh_token" \
  -d "scope=https://www.googleapis.com/auth/drive.file"
```
Si responde JSON con `access_token`, el refresh token es válido.

### Paso 8 — Configurar el servidor
Añadir al entorno del backend (`.env` en prod / variables del runner):
```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REFRESH_TOKEN=...
GOOGLE_DRIVE_ROOT_FOLDER_ID=...
```

### Notas y cuidados
- **Pérdida/revocación del token:** cambiar la contraseña de la cuenta, quitar
  el acceso desde *myaccount.google.com → Security → Third-party apps*, o
  actividad sospechosa revocan el token. Solución: repetir pasos 5 y 8.
- **Cuota de almacenamiento:** los archivos cuentan contra la cuenta Gmail
  (15 GB gratis; Google One si se requiere más).
- El refresh token **pertenece a la cuenta que autorizó**. Si esa cuenta se
  cierra, hay que re-autorizar con otra y re-compartir la carpeta raíz.

## 8. Fases de implementación

| Fase | Contenido | Rama sugerida |
|------|-----------|---------------|
| 0 | Setup Google Cloud + refresh token (tutorial §7) por el usuario | — |
| 1 | BD: DDL + seed + columna `alumnos.documentos_folder_id` | `feat/docs-drive-db` |
| 2 | Backend: deps, config, `GoogleDriveService`, endpoints, seguridad, tests | `feat/docs-drive-api` |
| 3 | Frontend: `app-icon` + reemplazo de iconos en todas las tablas | `feat/icons-globales` |
| 4 | Frontend: modal documentos, servicio, guard/sidebar para servicios_escolares | `feat/docs-drive-ui` |
| 5 | Pruebas E2E manuales (ver §9) + docs (README, `.env.example`, `dbdoc`) | `feat/docs-drive-polish` |

## 9. Criterios de aceptación / pruebas manuales

- [ ] Subir PDF y JPG desde el modal → aparecen en Drive dentro de
      `Documentacion/{CURP}/` y en la lista del modal.
- [ ] Abrir "en Drive" funciona con la sesión del humano (archivo visible).
- [ ] Eliminar desde la app → desaparece del modal y de Drive.
- [ ] Subir archivo no permitido (ej. .exe) o > 20 MB → error claro en español.
- [ ] Si falla la BD tras subir a Drive, el archivo no queda "huérfano" en Drive.
- [ ] Usuario `servicios_escolares`: ve tabla de alumnos + documentación, pero
      NO ve botones de crear/editar/eliminar alumnos.
- [ ] Iconos con `title` en todas las tablas; tooltip visible.
- [ ] Sin regresiones: CRUD existente intacto (`./mvnw test`, build de Angular).

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Refresh token revocado | Procedimiento de rotación documentado (§7); error 502 con mensaje claro |
| Cuota de 15 GB de la cuenta Gmail | Monitorear uso; Google One; los archivos viven en la cuenta humana (ampliable) |
| App "no verificada" (aviso) | Aceptado: scope no sensible, uso interno; aviso solo al autorizar |
| Orfandad de archivos (Drive↔BD) | Compensación: borrar en Drive si falla el INSERT |
| `servicios_escolares` con acceso de más | Matcher solo `GET /alumnos/**`; botones de mutación ocultos por rol |
| Migración de BD de dev existente | DDL manual o recrear volumen; documentado en §4 |
| Archivos manuales en Drive no visibles en la app | Aceptado por decisión (unidireccional); enlace "abrir en Drive" los cubre |

## 11. Decisión abierta

¿`servicios_escolares` debe tener CRUD completo de alumnos o solo lectura +
documentos? **Recomendado (ya reflejado en el plan):** lectura de alumnos +
gestión completa de documentos; mutaciones de alumnos siguen siendo solo admin.

## 12. Fuera de alcance (v1)

- Visor/preview de archivos dentro de la app (se usa el enlace a Drive).
- Sincronización bidireccional Drive → app.
- Versionado/reemplazo de documentos del mismo tipo.
- Múltiples carpetas raíz o varias cuentas autorizadas.
