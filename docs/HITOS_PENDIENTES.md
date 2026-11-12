# Hitos pendientes — Gestión Universitaria CUCII

> Documento de planeación vigente al **2026-11-12**.
> Sustituye la vista de pendientes de `PLAN_BETA.md` para el trabajo posterior al
> kit de despliegue.
>
> Convenciones: `server/AGENTS.md` y `view/AGENTS.md`.
> Código en inglés; texto visible al usuario en español.

---

## 1. Estado de partida

| Hito | Estado | Qué incluye |
|------|--------|-------------|
| **H0 — Higiene y configuración** | ✅ Completado | `.gitignore`, variables de entorno, perfil `prod`, `environment.prod.ts`, cookies condicionales, CORS configurable, filtro JWT, rename del repo. |
| **H1 — Funcionalidad mínima beta** | ✅ Completado | Búsqueda/filtros/orden server-side, validación alumno↔grupo, catálogos en selects, errores globales, archivado/restauración en todos los CRUDs. |
| **HA — Kit de despliegue** | ✅ Completado y validado localmente | Dockerfile del API, `docker-compose.prod.yaml` (db + api + caddy), Caddyfile, base limpia + bootstrap de admin/docente, backups, guía `docs/DEPLOY.md`. |

**Pendiente externo inmediato:** aprovisionar el VPS y crear el DNS de
`api.gestion.cucii.mx`. Sin eso no se puede ejecutar el primer despliegue real,
aunque el kit ya está probado en local.

> `main` sigue detrás de `develop`. Antes de desplegar hay que hacer el merge
> `develop → main` y desplegar desde `main`.

---

## 2. Resumen de hitos pendientes

| ID | Hito | Corresponde a | Prioridad | Depende de | Estimación |
|----|------|---------------|-----------|------------|------------|
| **HB** | Despliegue real en VPS + Cloudflare Pages | `PLAN_BETA` 2.4.1, 2.7 y 3.2 | P0 | VPS y DNS | 0.5–1 día |
| **HC** | Operación segura de calificaciones | `PLAN_BETA` 3.2 + calidad operativa | P0 | HA/HB | 1–1.5 días |
| **HD** | Carga masiva CSV de alumnos y calificaciones | `PLAN_BETA` 3.4 y 3.5 | P0 | HC | 2–3 días |
| **HE** | Continuidad de datos: migraciones y backups externos | `PLAN_BETA` 2.5 y 2.6 | P0 | HB | 1 día |
| **HF** | Endurecimiento, observabilidad y CI/CD | `PLAN_BETA` Fase 4 | P1 | HB | 1–2 días |
| **HG** | Matriz de roles y alcance por docente/grupo/materia | `PLAN_BETA` Fase 4 | P1 | HC | 2–3 días |
| **HH** | Ciclos escolares e historial por periodo | `PLAN_BETA` Fase 4 | P2 | HG | 3–5 días |
| **HI** | Kardex/boleta y exportación PDF/CSV | `PLAN_BETA` Fase 4 | P2 | HH | 3–5 días |
| **HJ** | Google Drive para documentación de alumnos + iconos | `PLAN_DRIVE_DOCUMENTACION.md` | P2 | HG | 3–5 días |
| **HK** | Pruebas backend/frontend y e2e | `PLAN_BETA` Fase 4 | P1 continuo | HC | continuo |
| **HL** | Cierre de beta: documentación final y tag `v0.1.0-beta` | `PLAN_BETA` 3.3 | P1 | HB–HD | 0.5 día |

Las estimaciones son preliminares y no incluyen tiempos de espera de DNS,
certificados o aprovisionamiento del VPS.

---

## 3. Detalle de cada hito

### HB — Despliegue real en VPS + Cloudflare Pages

**Objetivo:** poner la app disponible en `https://gestion.cucii.mx` y el API en
`https://api.gestion.cucii.mx` para iniciar la captura real.

**Alcance**
- Aprovisionar VPS (Ubuntu, Docker + Compose, firewall 22/80/443).
- Crear registro DNS `api` → IP del VPS, **DNS only** en Cloudflare.
- Clonar `main` en el VPS, crear `.env.prod` y levantar el stack.
- Crear el proyecto de Cloudflare Pages (root `view`, build
  `pnpm install && pnpm build`, output `dist/calificaciones/browser`,
  `NODE_VERSION=22`).
- Apuntar `gestion.cucii.mx` a Pages y validar CORS.
- Smoke test: login admin, login docente, alta de alumno, asignación a grupo,
  calificación manual, consulta en `/browse`.
- Cambiar la contraseña del admin después del primer login.

**Criterios de aceptación**
- `https://api.gestion.cucii.mx/actuator/health` responde `UP`.
- `https://gestion.cucii.mx` carga el login y permite autenticarse.
- El docente puede consultar y registrar calificaciones; no puede entrar a los
  CRUDs administrativos.
- Los puertos 5432 y 8080 no están expuestos al exterior.
- Existe un respaldo inicial generado y validado.

**Corresponde a:** `PLAN_BETA` 2.4.1, 2.7 y 3.2. Cierra el Hito A en producción.

---

### HC — Operación segura de calificaciones

**Objetivo:** que la captura diaria sea corregible y consistente antes de cargar
datos en volumen.

**Alcance**
1. **Editar y eliminar calificaciones desde la UI**:
   - `GradesService.update/delete` ya existen pero ninguna pantalla los usa.
   - Agregar acciones en `/browse` (detalle del alumno) o en `/upload`.
   - Confirmación antes de eliminar y toast de éxito.
2. **Reactivar registros archivados** en lugar de bloquear:
   - `alumnos_grupos`, `calificaciones` y `profesores_grupos` tienen `UNIQUE` y
     soft-delete. Al reasignar o volver a registrar, hoy responde “ya existe”.
   - Al crear, si existe la fila inactiva, reactivarla (`is_active = true`).
3. **Estatus inicial “Cursando”** en el formulario de alumnos (hoy inicia en
   “Invasión”).
4. **`registradoPor` seguro** en `/upload`: evitar enviar `0` si la sesión aún no
   cargó; deshabilitar el submit o resolver el usuario antes.
5. Revisar la inconsistencia `calificaciones.registrado_por NOT NULL` +
   `ON DELETE SET NULL`.

**Criterios de aceptación**
- Se puede corregir una calificación sin tocar la base de datos.
- Quitar y volver a agregar un alumno a un grupo funciona.
- Volver a registrar una calificación eliminada funciona.
- Si el usuario aún no está hidratado, la UI no envía `registradoPor = 0`.

**Corresponde a:** calidad operativa derivada de `PLAN_BETA` 3.2; no estaba
listado como entregable propio en la versión anterior del plan.

---

### HD — Carga masiva CSV de alumnos y calificaciones

**Objetivo:** cargar la primera generación de datos sin capturar alumno por
alumno ni calificación por calificación.

**Alcance**
1. **Backend batch de alumnos** (`POST /alumnos/batch`):
   - Transaccional o con reporte de errores por fila.
   - Validar CURP, correo único, estatus y duplicados.
   - Devolver resumen: insertados, omitidos, errores con número de fila.
2. **Backend batch de calificaciones** (`POST /calificaciones/batch`):
   - Validar alumno↔grupo, materia del plan/cuatrimestre, rango 0–100 y
     duplicados.
   - Reporte de errores por fila sin abortar todo el lote (o modo transaccional
     configurable).
3. **Frontend `/upload`**:
   - Selector de archivo CSV, previsualización, validación y confirmación.
   - Plantilla descargable con columnas exactas.
   - Resumen de resultados y errores por fila.
   - `addMany` en `GradesService` y `StudentsService`.
4. **Plantillas**:
   - Alumnos: nombres, primer apellido, segundo apellido, CURP, correo,
     estatus.
   - Calificaciones: CURP, grupo (clave), materia (clave), calificación.

**Criterios de aceptación**
- Un archivo con filas válidas e inválidas inserta las válidas y reporta las
  inválidas con número de fila.
- No se duplican alumnos ni calificaciones.
- La plantilla y la previsualización coinciden con las columnas del backend.
- El docente puede usar la carga de calificaciones si tiene permiso.

**Corresponde a:** `PLAN_BETA` 3.4 y 3.5. **Ambos son requeridos**; 3.5 deja de
ser opcional.

---

### HE — Continuidad de datos: migraciones y backups externos

**Objetivo:** poder cambiar el esquema y recuperar la base sin perder la
información real.

**Alcance**
1. Adoptar **Flyway** o un directorio `database/migrations/` con numeración y
   script de aplicación.
2. Congelar la línea base actual (`V1__schema.sql`) y documentar cómo agregar
   cambios.
3. Configurar copia de respaldos **fuera del VPS** (S3, Backblaze, otro host).
4. Programar y probar un **restore completo** en un entorno limpio.
5. Documentar el procedimiento de rollback de aplicación y de esquema.

**Criterios de aceptación**
- Un cambio de esquema se aplica sin borrar el volumen.
- Existe al menos un restore probado y documentado.
- El respaldo diario se copia fuera del VPS y falla ruidosamente si no puede.

**Corresponde a:** `PLAN_BETA` 2.5 y 2.6. El respaldo local ya existe; falta la
copia externa, el restore probado y las migraciones.

---

### HF — Endurecimiento, observabilidad y CI/CD

**Objetivo:** reducir riesgo operativo y detectar fallas antes de que afecten la
captura.

**Alcance**
- Deshabilitar Swagger/OpenAPI en producción (`springdoc.*.enabled=false`).
- Rate limiting o bloqueo progresivo en `/auth/login`.
- Revisar expiración de JWT, rotación de secretos y cookies.
- Logs estructurados, retención y alertas básicas.
- `/actuator/health` ya existe; agregar métricas/liveness si se necesita.
- Pipeline de CI: build backend, build frontend, tests y (opcional) deploy.
- Revisar headers de seguridad en Caddy y Cloudflare.

**Criterios de aceptación**
- Swagger no responde en producción.
- El login tiene alguna protección contra fuerza bruta.
- Un push a `develop` ejecuta build y tests automáticos.
- Los logs permiten identificar errores 5xx sin revisar el contenedor a mano.

**Corresponde a:** `PLAN_BETA` Fase 4 (CI/CD y observabilidad) + hallazgos de
seguridad del análisis de despliegue.

---

### HG — Matriz de roles y alcance por docente/grupo/materia

**Objetivo:** que cada rol vea y modifique solo lo que le corresponde.

**Alcance**
- Definir permisos para `admin`, `rector`, `docente`, `servicios_escolares` y
  `coordinador`.
- Restringir al docente a sus grupos/materias asignados en
  `profesores_grupos`.
- Alinear `adminGuard`/sidebar del frontend con el backend.
- Agregar tests de autorización por rol.
- Resolver la brecha actual: el docente puede consultar todos los alumnos y
  calificaciones, no solo los de sus grupos.

**Criterios de aceptación**
- Un docente no puede leer ni calificar grupos ajenos.
- `rector`, `servicios_escolares` y `coordinador` tienen un alcance definido y
  probado.
- El frontend no muestra acciones que el backend rechazará.

**Corresponde a:** `PLAN_BETA` Fase 4 (matriz de roles real).

---

### HH — Ciclos escolares e historial por periodo

**Objetivo:** separar la operación por periodo y conservar historial.

**Alcance**
- Agregar periodo/ciclo a grupos (o una entidad `ciclos_escolares`).
- Definir reglas de inscripción y calificaciones por periodo.
- Migrar el `UNIQUE (alumno, materia, grupo)` si se requiere más de un registro
  histórico.
- Pantallas para abrir/cerrar periodos y consultar historial.

**Criterios de aceptación**
- Un alumno puede tener calificaciones de la misma materia en periodos
  distintos.
- El periodo activo se selecciona de forma explícita en captura y consulta.
- No se mezclan calificaciones de periodos diferentes.

**Corresponde a:** `PLAN_BETA` Fase 4 (ciclos escolares e historial).

---

### HI — Kardex/boleta y exportación PDF/CSV

**Objetivo:** entregar documentos académicos a alumnos y servicios escolares.

**Alcance**
- Vista de kardex/boleta por alumno y periodo.
- Exportación PDF y CSV.
- Encabezados institucionales, promedios y estatus.
- Permisos por rol.

**Criterios de aceptación**
- El PDF coincide con las calificaciones vigentes.
- El CSV se puede abrir en Excel sin errores de codificación.
- Solo los roles autorizados pueden generarlos.

**Corresponde a:** `PLAN_BETA` Fase 4 (kardex/boleta y export).

---

### HJ — Google Drive para documentación de alumnos + iconos

**Objetivo:** subir documentos de alumnos a la carpeta compartida de Drive y
sustituir textos de acción por iconos.

**Estado:** **excluido por ahora**. No hay código de Drive en el repo; solo el
documento de planeación `PLAN_DRIVE_DOCUMENTACION.md`.

**Alcance** (ver plan completo)
- OAuth 2.0 con refresh token, scope `drive.file`.
- Catálogo `tipos_documento`, tabla de documentos, endpoints multipart.
- `GoogleDriveService`, carpeta por CURP, compensación BD/Drive.
- Componente `app-icon` y migración de botones a iconos.
- Roles `admin` + `servicios_escolares`.

**Dependencias**
- Requiere la matriz de roles (**HG**) para incorporar
  `servicios_escolares`.
- Requiere definir la cuenta de Google y el refresh token en producción.
- No bloquea la beta de calificaciones.

**Corresponde a:** `PLAN_DRIVE_DOCUMENTACION.md` completo.

---

### HK — Pruebas backend/frontend y e2e

**Objetivo:** proteger los flujos críticos mientras el sistema crece.

**Alcance**
- Tests de servicios/repositorios críticos: calificaciones, inscripción,
  archivado, autenticación.
- Tests de autorización por rol.
- Tests de componentes/servicios Angular.
- E2E de los flujos: login → browse → upload manual → CSV → CRUDs admin.
- Integración en CI.

**Criterios de aceptación**
- Los flujos críticos tienen cobertura automatizada.
- Un cambio que rompe login, inscripción o calificaciones falla en CI.

**Corresponde a:** `PLAN_BETA` Fase 4 (tests backend/e2e).

---

### HL — Cierre de beta

**Objetivo:** dejar un punto estable y documentado para operar.

**Alcance**
- Completar smoke test end-to-end en producción.
- Actualizar README, AGENTS, PLAN_BETA y este documento.
- Revisar que no queden referencias a `calificaciones.cucii.mx` ni secretos.
- Tag `v0.1.0-beta` en `main`.
- Definir el proceso de soporte y respaldos.

**Criterios de aceptación**
- Tag creado y desplegado.
- Documentación alineada con la versión desplegada.
- Backups y restore verificados.
- Responsable de operación identificado.

**Corresponde a:** `PLAN_BETA` 3.3 y cierre de Fase 3.

---

## 4. Orden recomendado

```
HB (despliegue real)
  ↓
HE (migraciones + backup externo + restore)
  ↓
HC (editar/eliminar calificaciones + reactivaciones)
  ↓
HD (CSV de alumnos y calificaciones)
  ↓
HF (endurecimiento + CI/CD)  ── en paralelo con HK
  ↓
HG (roles finos)
  ↓
HH (ciclos) → HI (kardex)
  ↓
HJ (Google Drive + iconos)
  ↓
HL (tag v0.1.0-beta)
```

**Antes de capturar datos reales en volumen** deben estar cerrados, como mínimo:
HB, HE y HC. HD puede hacerse con la primera carga o antes de la segunda.

---

## 5. Decisiones abiertas

| Tema | Qué falta decidir |
|------|-------------------|
| VPS | Proveedor, región, IP y acceso SSH |
| Cloudflare Pages | Cuenta/proyecto y dominio definitivo |
| CSV | Formato exacto de columnas y si la carga es transaccional o parcial |
| Docente | ¿Puede editar/eliminar calificaciones o solo registrar? |
| Roles | Alcance de rector, coordinador y servicios escolares |
| Ciclos | ¿Un ciclo por año/cuatrimestre? ¿Se requiere historial desde la primera carga? |
| Drive | Cuenta de Google, carpeta raíz y responsable del refresh token |
| Licencia | El README menciona MIT pero no existe archivo `LICENSE`; definir titular y añadirlo o retirar la mención |

---

## 6. Referencias

- [`../PLAN_BETA.md`](../PLAN_BETA.md) — plan original por fases.
- [`../PLAN_DRIVE_DOCUMENTACION.md`](../PLAN_DRIVE_DOCUMENTACION.md) — plan de Drive (Hito HJ).
- [`DEPLOY.md`](DEPLOY.md) — guía de despliegue del Hito HA/HB.
- [`../server/AGENTS.md`](../server/AGENTS.md) — convenciones y endpoints del backend.
- [`../view/AGENTS.md`](../view/AGENTS.md) — convenciones del frontend.
- [`../database/init/README.md`](../database/init/README.md) — inicialización de la base.
