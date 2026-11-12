# Gestión Universitaria CUCII — Frontend

SPA en Angular para la gestión académica del CUCII: alumnos, grupos, planes de
estudio, materias, planteles, usuarios y calificaciones.

## Stack

| Tecnología | Versión |
|------------|---------|
| Angular | 21.2 |
| TypeScript | 5.9 |
| TailwindCSS | 4.2 |
| RxJS | 7.8 |
| Gestión de estado | Angular Signals (sin NgRx) |

La app es **zoneless**, usa componentes standalone y rutas lazy-loaded.

## Requisitos

- Node.js 20.19+ / 22.12+ / 24+
- pnpm 9+

## Desarrollo

```bash
pnpm install
pnpm start
```

La app queda en `http://localhost:4200` y consume el API de
`http://localhost:8080` (`src/environments/environment.ts`).

## Build de producción

```bash
pnpm build
```

Salida: `dist/calificaciones/browser`.

El build de producción reemplaza `environment.ts` por `environment.prod.ts`
(`https://api.gestion.cucii.mx`) mediante `fileReplacements` en `angular.json`.

## Rutas

| Ruta | Acceso | Descripción |
|------|--------|-------------|
| `/login` | Público | Inicio de sesión |
| `/browse` | Autenticado | Consulta de alumnos y calificaciones |
| `/upload` | Autenticado | Registro manual de calificaciones |
| `/students` | Admin | CRUD de alumnos |
| `/profesores` | Admin | Asignación profesor ↔ grupo ↔ materia |
| `/groups` | Admin | CRUD de grupos |
| `/programs` | Admin | CRUD de planes de estudio |
| `/subjects` | Admin | CRUD de materias |
| `/campuses` | Admin | CRUD de planteles |
| `/users` | Admin | CRUD de usuarios |
| `/groups/:id/students` | Admin | Alumnos del grupo |

## Autenticación

- El token JWT se guarda en la cookie `auth_token` (SameSite=Lax, `Secure` solo
  sobre HTTPS, 24 h).
- `auth_role` guarda el `rolId` para restaurar el rol mientras `/auth/me`
  resuelve.
- El interceptor agrega `Authorization: Bearer <token>` y hace logout +
  redirect a `/login` ante un `401`.
- `adminGuard` restringe las pantallas administrativas a `admin`.
- El rol `docente` accede a `/browse` y `/upload`; los CRUDs quedan para admin.

## Documentación relacionada

- Convenciones del frontend: [`AGENTS.md`](./AGENTS.md)
- API REST: `server/README.md` y `docs/api-docs.json`
- Despliegue en Cloudflare Pages: [`../docs/DEPLOY.md`](../docs/DEPLOY.md)
- Hitos pendientes: [`../docs/HITOS_PENDIENTES.md`](../docs/HITOS_PENDIENTES.md)

## Pendientes de frontend

- Importación CSV de alumnos y calificaciones (hito HD).
- Editar/eliminar calificaciones desde la UI (hito HC).
- Catálogos de selects limitados a 100 registros; migrar a typeahead cuando
  crezcan.
