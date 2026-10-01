# CLAUDE.md — Plataforma de Calificaciones CUCII

## Stack

- **Angular 20.3.0** — zoneless (`provideZonelessChangeDetection`), standalone components, lazy-loaded routes
- **TailwindCSS 4.2.1** — configurado vía `src/styles.css` con `@import "tailwindcss"`
- **TypeScript 5.9**, **RxJS 7.8**
- Sin NgRx ni librerías de estado externas — estado reactivo con **Angular Signals**
- Backend: Spring Boot, PostgreSQL. API definida en `docs/api-docs.json`

## Convenciones de idioma

- **Código en inglés, datos/contenido en español.** Nombres de clases, métodos, señales, variables, archivos y rutas van en inglés. Labels, placeholders, mensajes van en español.
- **Field names igual que el backend.** No hay mappers — los modelos del frontend usan los mismos nombres que la API (ej. `numeroRvoe`, `duracionCuatrimestres`, `alumnoId`, `nombre`). Los IDs son `number`.

## Roles (rolId mapeado en `auth.model.ts`)

| ID | Rol | Acceso |
|----|-----|--------|
| 1 | `admin` | Total (sidebar completo) |
| 2 | `rector` | Total (sidebar completo) |
| 3 | `docente` | Básico (browse + upload) |
| 4 | `servicios_escolares` | Sin detalle aún |
| 5 | `coordinador` | Sin detalle aún |

`AuthService.isAdmin()` retorna `true` para `admin` y `rector`.

## Autenticación

- Token JWT guardado en cookie `auth_token` (JS-accessible, SameSite=Lax, Secure, 24h)
- `rolId` guardado en cookie `auth_role` para restaurar rol inmediatamente al recargar
- `GET /auth/me` se llama al recargar (vía `setTimeout(0)`) para refrescar datos completos del usuario
- `AuthService` expone señal `_cachedRole` que se hidrata sincrónicamente desde cookie `auth_role` en el constructor. `isAdmin()` usa `currentUser()?.rol ?? _cachedRole()` como fallback, evitando race condition con guards que corren antes de que `/auth/me` resuelva.
- Sin `POST /auth/logout` — el logout del frontend limpia las cookies
- Interceptor HTTP agrega `Authorization: Bearer <token>` y captura 401 → logout + redirect a `/login`

## Modelos (`src/app/core/models/`)

Todos los field names coinciden 1:1 con los DTOs del backend:

```typescript
// auth.model.ts
LoginResponse { id, nombre, apellido, email, rolId, token }
MeResponse    { id, nombre, apellido, email, rolId }
AuthUser      { id, nombre, apellido, email, rolId, rol: UserRole }
UserRole      = 'admin' | 'rector' | 'docente' | 'servicios_escolares' | 'coordinador'

// user.model.ts
User          { id, nombre, apellido, email, rolId, plantelId, isActive }
UsuarioRequest { nombre, apellido, email, password, rolId, plantelId }

// student.model.ts
Student       { id, nombres, primerApellido, segundoApellido?, curp, correoInstitucional, estatusId }
STATUS_MAP    Record<number, { label, classes }> — 1=Invasión (amarillo), 2=Cursando (verde), 3=Egresado (verde brillante), 4=Baja (rojo)

// program.model.ts
Program       { id, nombre, grado, numeroRvoe, fechaRvoe, duracionCuatrimestres, cantidadMaterias, materias: Subject[] }
Subject       { id, clave, nombre, cuatrimestre, creditos }

// group.model.ts
Group         { id, clave, nombre, planEstudioId, plantelId }

// campus.model.ts
Campus        { id, nombreOficial, nombreCorto?, direccionCalle?, direccionNumeroExt?,
                direccionNumeroInt?, colonia?, codigoPostal?, ciudadMunicipio,
                estado, pais?, directorNombre? }

// grade.model.ts
Grade         { id, alumnoId, grupoId, materiaId, calificacion, registradoPor }

// group-student.model.ts
GroupStudent  { id?, groupId, studentId }   // id se usa para DELETE

// teacher-assignment.model.ts
TeacherAssignment { id, userId, groupId, subjectId }
```

## Servicios (`src/app/core/services/`)

Todos usan `HttpClient` con endpoints reales. `signal<T[]>` interno, expuesto como `.asReadonly()`.

Los métodos retornan `Observable<T>` con HTTP real, no `of()` mock.

**Paginación y filtrado server-side:** cada servicio con listas expone `loadPage(page, size?, filters?)` donde `filters` es un `QueryFilters` (ver `core/services/http-params.ts`). El servicio guarda el último `filters` y lo reutiliza al paginar o al recargar tras un `add/update/delete`. El backend soporta `?search=`, filtros por FK, `isActive`, `sortBy`, `sortDir` en todos los GET paginados. Para búsquedas puntuales que no deben tocar el signal (ej. modal de alumnos en Upload) usar métodos dedicados como `searchStudents(query)`.

| Servicio | Endpoints base | Notas |
|---|---|---|
| `AuthService` | `/auth/login`, `/auth/me` | Cookie + signal. `login()` → setea cookies + currentUser. Constructor restaura desde cookies + valida con `/auth/me`. `_cachedRole` fallback desde cookie `auth_role` para guards sincrónicos. |
| `UsersService` | `/usuarios` | CRUD completo. `add`/`update` esperan `UsuarioRequest`. `loadPage` con `?search=`. |
| `StudentsService` | `/alumnos` | CRUD completo. `searchStudents(query)` → `/alumnos?search=` (no muta el signal, ideal para modales). `loadPage` con `?search=`/`?curp=`. |
| `GradesService` | `/calificaciones` | `getByStudent(alumnoId)` → `/calificaciones?alumnoId=&size=100` (respuesta paginada). Sin `addMany` (pendiente batch, Fase 3). |
| `ProgramsService` | `/planes-estudio/con-materias-count` (lista), `/planes-estudio/{id}/con-materias` (detalle) | `loadPage` con `?search=`. Subjects se cargan separado vía `getSubjectsByProgram()`. |
| `GroupsService` | `/grupos` | CRUD + `getCuatrimestresCount(id)` → `/grupos/{id}/cuatrimestres` + `getSubjectsByGroupAndTerm(groupId, cuatri)` → `/grupos/{groupId}/cuatrimestres/{cuatri}/materias` + `getByIds(ids)` (forkJoin) + `getByProgram(planEstudioId)` |
| `CampusesService` | `/planteles` | CRUD, `loadPage` con `?search=` |
| `GroupStudentsService` | `/alumnos-grupos`, `/grupos/{id}/alumnos` | Mapea `alumnoId`↔`studentId`, `grupoId`↔`groupId`. `getStudentsByGroup(grupoId)` (asignados), `getAvailableStudents(grupoId, page, size, search)` (disponibles), `getByStudent(alumnoId)`, `removeByGroupAndStudent(grupoId, studentId)`. |
| `TeacherAssignmentsService` | `/profesores-grupos` | Mapea respuesta enriquecida (`usuarioNombre`, `grupoClave/Nombre`, `materiaClave/Nombre`). `loadPage` con `?search=` (profesor/grupo/materia) y filtros FK. `add` recibe `{ userId, groupId, subjectId }` y devuelve `void`. |

## Rutas (`src/app/app.routes.ts`)

| Path | Componente | Guard |
|---|---|---|
| `/` | redirect → `/browse` | — |
| `/login` | `Login` | — |
| `/browse` | `Browse` | `authGuard` |
| `/upload` | `Upload` | `authGuard` |
| `/students` | `Students` | `adminGuard` |
| `/profesores` | `Profesores` | `adminGuard` |
| `/groups` | `Groups` | `adminGuard` |
| `/programs` | `Programs` | `adminGuard` |
| `/subjects` | `Subjects` | `adminGuard` |
| `/campuses` | `Campuses` | `adminGuard` |
| `/users` | `Users` | `adminGuard` |
| `/groups/:id/students` | `GroupStudents` | `adminGuard` |
| `**` | redirect → `/browse` | — |

## Convenciones de código

- Componentes: `inject()` en clase, signals para estado local, `computed()` para derivados
- **No usar `ngOnInit`** — inicialización en constructor o inline
- Forms: `FormBuilder.nonNullable.group({})` siempre
- **Búsqueda en listas**: patrón draft+committed — `filterDraft` (input) + `filterQ` (aplicado). `search()` dispara `service.loadPage(0, pageSize(), q ? { search: q } : {})`; el filtrado es **server-side**, `filtered` solo refleja `service.list()`. El servicio recuerda los filtros para paginar y recargar.
- **Formularios unificados**: un solo `form` (NO `addForm` + `editForm` separados). Control de modo vía `editingId = signal<number | null>(null)`:
  - `null` = modo creación, `<id>` = modo actualización
  - `startEdit(item)`: setea `editingId`, carga valores en `form` con `setValue()`, muestra el formulario
  - `closeForm()`: limpia `editingId`, resetea `form`, oculta
  - `toggleForm()`: alterna entre abrir/cerrar formulario
  - `submit()`: si `editingId()` → `update()`, si no → `add()`, luego `closeForm()`
- `startEdit()`: usar `?? ''` en todos los valores de `setValue()` para evitar error de `NonNullableFormBuilder` con `undefined`
- `closeForm()` siempre llama `form.reset()` para restaurar defaults del form builder
- Templates: `@if`, `@for`, `@empty`, `@else` (control flow de Angular 17+, no directivas estructurales)
- Inputs sin two-way: `[value]="signal()"` + `(input)="signal.set($any($event.target).value)"`

## Flujo Upload (manual)

1. Buscar alumno → `studentsService.searchStudents(query)` → `/alumnos?search=` server-side → modal de resultados
2. Seleccionar alumno → `groupStudentsService.getByStudent(student.id)` → grupos vía `groupsService.getByIds(...)`
3. Seleccionar grupo → `groupsService.getCuatrimestresCount(groupId)` → llena select de cuatrimestres
4. Seleccionar cuatrimestre → `groupsService.getSubjectsByGroupAndTerm(groupId, term)` → llena materias
5. Ingresar calificación → `gradesService.addGrade({ alumnoId, grupoId, materiaId, calificacion, registradoPor })`
6. `registradoPor` = `auth.currentUser()?.id`

## Flujo Browse

- Lista: `studentsService.loadPage(0, size, { curp, search, isActive })` — filtrado y paginado server-side
- Detalle: al seleccionar alumno, `groupStudentsService.getByStudent()` → `groupsService.getByIds()` → `planEstudioId` → `programsService.getSubjectsByProgram()`; se cruza con `gradesService.getByStudent(alumnoId)` (respuesta paginada)
- Agrupado por `cuatrimestre` de cada `Subject`. Sin `cuatrimestre` en `Grade` — se deriva de la materia.

## Pendientes conocidos (frontend)

- Los servicios son singletons: aplicar filtros en una pantalla deja el signal filtrado hasta que otra pantalla lo recargue.
- Selects de catálogo en formularios (`Profesores`) usan `size=100`; si un catálogo supera 100 registros habrá que migrarlos a typeahead server-side.

## Flujo Profesores (`/profesores`)

- Lista: `TeacherAssignmentsService.loadPage(0, size, { search })` → `/profesores-grupos` ya devuelve los nombres resueltos (profesor, grupo, materia); no hay que cruzar catálogos en cliente.
- Alta: selects de profesor (`UsersService.getTeachers()` → `/usuarios?rolId=3&size=100`), grupo (`GroupsService.getAll()` → `/grupos?size=100`) y materia (según el plan del grupo vía `ProgramsService.getSubjectsByProgram()`).
- `add` no devuelve la entidad; errores de duplicado se muestran por el callback `error` (HTTP 400).

## Flujo GroupStudents (`/groups/:id/students`)

- Asignados: `groupStudentsService.getStudentsByGroup(groupId)` → `/grupos/{id}/alumnos` (server-side, solo activos).
- Disponibles: `getAvailableStudents(groupId, page, size, search)` → `/grupos/{id}/alumnos-disponibles` (paginado y con búsqueda server-side; excluye asignados activos).
- `assign` → `POST /alumnos-grupos`; `remove` → `removeByGroupAndStudent` (busca la asignación por `alumnoId`+`grupoId` y borra por id).
- Tras asignar/quitar se recargan ambas listas.

## Comandos

```bash
ng build --configuration development   # build rápido (~1.2s), sin optimización
ng serve                               # dev server
```
