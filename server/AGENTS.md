# AGENTS.md — Plataforma de Calificaciones CUCII (server)

## Stack

- **Spring Boot 4.0.6** — Java 21, Maven (mvnw wrapper), devtools
- **JdbcTemplate + RowMapper** — SQL explícito en repositorios. NO Spring Data JDBC, no JPA, no lazy loading.
- **Spring Security 7** — stateless JWT, `SecurityFilterChain` con lambda DSL
- **PostgreSQL 18** via Docker (`database/docker-compose.yaml`)
- **Lombok** — `@RequiredArgsConstructor` es el patrón de inyección estándar (los LSP errors sobre "blank final field" son falsos positivos del LSP con Lombok)
- **JJWT 0.12.6** — HS256, firma con `Keys.hmacShaKeyFor`

## Convención de idioma

**Código en inglés, texto visible al usuario en español.** Nombres de clases, métodos, campos, rutas URL, archivos. Mensajes de error HTTP en español. `curp` y `rvoe` son acrónimos oficiales mexicanos, se conservan en minúsculas sin traducción.

## Comandos

| Comando | Descripción |
|---------|-------------|
| `./mvnw spring-boot:run` | Arranca servidor en :8080 |
| `./mvnw compile` | Compilar sin ejecutar tests |
| `./mvnw test` | Todos los tests |
| `./mvnw test -Dtest=NombreTest` | Test específico |
| `./mvnw package -DskipTests` | Genera JAR |

## Arquitectura en capas

```
Controller (HTTP)
    ↓ llama
Service (lógica de negocio, mapeo a DTOs)
    ↓ llama
Repository (JdbcTemplate, SQL explícito)
    ↓
PostgreSQL
```

```
mx.cucii.school.platform/
├── config/        SecurityConfig — SecurityFilterChain, CORS, PasswordEncoder, AuthenticationManager
├── controller/    @RestController — solo delegan al service
├── dto/           Records request/response (nunca se expone el model directamente)
├── exception/     ResourceNotFoundException, GlobalExceptionHandler (@RestControllerAdvice)
├── model/         Java records con @Table, @Id, @Column (redundantes para JdbcTemplate, se mantienen como documentación)
├── repository/    Clases @Repository con JdbcTemplate + RowMapper; sufijo *JdbcRepository
├── security/      JwtAuthenticationFilter (OncePerRequestFilter)
└── service/       Lógica de negocio; write methods son @Transactional. Timestamps se asignan aquí, no en la BD.
```

### Controller (@RestController)
- **Única responsabilidad:** manejar tráfico HTTP (GET, POST, PUT, DELETE).
- Recibe el request, valida formato (DTO), delega al Service, empaqueta respuesta con `ResponseEntity`.
- **Cero lógica de negocio, cero SQL, cero acceso a BD.**

### Service (@Service)
- **Única responsabilidad:** lógica de negocio y orquestación.
- Valida reglas (permisos, duplicados, rangos), asigna timestamps (`OffsetDateTime.now()`).
- Coordina operaciones entre múltiples repositorios (ej. cascade soft-delete: materias → plan).
- Mapea model records → DTOs de respuesta.
- **Cero SQL. No sabe qué base de datos hay detrás.**
- Métodos write están anotados con `@Transactional`.

### Repository (@Repository)
- **Única responsabilidad:** hablar con la base de datos.
- Recibe/retorna model records (`PlanEstudio`, `Materia`, `Usuario`, `Rol`, etc.).
- Usa `JdbcTemplate` con `RowMapper` para transformar filas SQL en records Java.
- SQL explícito en todas las operaciones.
- **Nombres:** `*JdbcRepository` (ej. `PlanEstudioJdbcRepository`, `UsuarioJdbcRepository`).
- **Cero lógica de negocio.** El repository no decide si una operación es válida, solo ejecuta la query.

### Model
- Java records inmutables.
- Anotaciones `@Table`, `@Id`, `@Column` heredadas de Spring Data JDBC — son redundantes con JdbcTemplate pero se mantienen como documentación de la estructura de la tabla.
- **IDs generados con `GENERATED ALWAYS AS IDENTITY`** — al insertar desde Java el campo `id` debe ser `null`.

## Patrón estándar de repositorio JdbcTemplate

```java
@Repository
public class EntidadJdbcRepository {

    private static final RowMapper<Entidad> MAPPER = (rs, rowNum) ->
        new Entidad(
                rs.getInt("id"),
                rs.getString("campo"),
                ...
        );

    private final JdbcTemplate jdbcTemplate;

    // Constructor (Spring injecta JdbcTemplate automáticamente)

    public List<Entidad> findAll() {
        return jdbcTemplate.query("SELECT * FROM tabla", MAPPER);
    }

    public List<Entidad> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM tabla ORDER BY id ASC LIMIT ? OFFSET ?",
                MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tabla", Long.class);
        return count != null ? count : 0;
    }

    public Optional<Entidad> findById(Integer id) {
        return jdbcTemplate.query("SELECT * FROM tabla WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public boolean existsById(Integer id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tabla WHERE id = ?", Integer.class, id
        ) > 0;
    }

    public Entidad save(Entidad entity) {
        if (entity.id() == null) {
            return jdbcTemplate.queryForObject(
                "INSERT INTO tabla (col1, col2, ...) VALUES (?, ?, ...) RETURNING *",
                MAPPER, entity.col1(), entity.col2(), ...
            );
        }
        return jdbcTemplate.queryForObject(
            "UPDATE tabla SET col1 = ?, col2 = ?, ... WHERE id = ? RETURNING *",
            MAPPER, entity.col1(), entity.col2(), ..., entity.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
            "UPDATE tabla SET is_active = false, updated_at = ? WHERE id = ?", now, id
        );
    }
}
```

### Soft delete
- **SQL directo:** `UPDATE tabla SET is_active = false, updated_at = ? WHERE id = ?`
- **Cascade soft-delete:** padre e hijos se desactivan en la misma `@Transactional` con dos `UPDATE` consecutivos.
- Ya no se carga el record en memoria, se reconstruye y se guarda de nuevo — se hace un solo `UPDATE`.
- **Tablas sin `updated_at`:** `alumnos_grupos` y `profesores_grupos` no tienen columna `updated_at`. El soft-delete en esas tablas es solo `UPDATE ... SET is_active = false WHERE id = ?`.

### Campos notables en la BD

| Tabla | Columna | Tipo | Java | Nota |
|-------|---------|------|------|------|
| `usuarios` | `apellido` | `varchar(100)` | `String apellido` | Nullable. |
| `planteles` | `latitud` | `numeric(10,8)` | `BigDecimal latitud` | Nullable. |
| `planteles` | `longitud` | `numeric(11,8)` | `BigDecimal longitud` | Nullable. |
| `planteles` | `pais` | `varchar(50)` | `String pais` | Default 'México'. Service asigna si es null. |
| `alumnos` | `curp` | `char(18)` | `String curp` | UNIQUE, validado con regex en service. |
| `alumnos` | `correo_institucional` | `varchar(120)` | `String correoInstitucional` | UNIQUE, nullable. |
| `alumnos` | `estatus_id` | `int` | `Integer estatusId` | FK a `estatus_alumnos.id`, NOT NULL. Service valida existencia. |
| `grupos` | `clave` | `varchar(20)` | `String clave` | UNIQUE. Auto-generada via `nextval('grupos_numero_seq')` → "CG-{n}". |
| `calificaciones` | `calificacion` | `numeric(5,2)` | `BigDecimal calificacion` | Rango 0-100. Validado en service. |
| `calificaciones` | `registrado_por` | `int` | `Integer registradoPor` | FK a `usuarios.id`, nullable, on delete set null. |

### Tablas sin `updated_at`
- `roles` — solo tiene `created_at`
- `alumnos_grupos` — solo tiene `created_at`
- `profesores_grupos` — solo tiene `created_at`

### FK chain: grupo → plan_estudio → materias/cuatrimestres
- `grupos.plan_estudio_id` → `planes_estudio.id` → `planes_estudio.duracion_cuatrimestres`
- Desde un `grupos.id` se obtiene la duración de la carrera y las materias por cuatrimestre.

### Cross-resource queries (FK traversal)
- **Services** pueden inyectar `*JdbcRepository` de otras entidades para navegar por FKs (ej. `GrupoService` inyecta `PlanEstudioJdbcRepository` y `MateriaJdbcRepository`).
- **Patrón:** buscar entidad padre → extraer FK → consultar repositorio hijo.
- Para joins simples en el mismo repositorio se puede usar `jdbcTemplate.query()` con la query JOIN. Para traer datos de entidades diferentes se inyecta el `JdbcRepository` de la entidad destino.

## Endpoints

### Planes de estudio (`/planes-estudio`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/planes-estudio` | Listar todos (paginado) |
| GET | `/planes-estudio/{id}` | Obtener por ID |
| GET | `/planes-estudio/{id}/con-materias` | Plan + materias activas (LEFT JOIN) |
| GET | `/planes-estudio/{id}/con-materias-sql` | Ídem (alias) |
| POST | `/planes-estudio` | Crear (201) |
| PUT | `/planes-estudio/{id}` | Actualizar |
| DELETE | `/planes-estudio/{id}` | Soft-delete + cascade a materias hijas (204) |

### Materias (`/materias`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/materias` | Listar todas (paginado) |
| GET | `/materias/{id}` | Obtener por ID |
| POST | `/materias` | Crear (201) |
| PUT | `/materias/{id}` | Actualizar |
| DELETE | `/materias/{id}` | Soft-delete (204) |

### Usuarios (`/usuarios`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/usuarios` | Listar todos (paginado, solo ADMIN) |
| GET | `/usuarios/{id}` | Obtener por ID |
| POST | `/usuarios` | Crear (201) |
| PUT | `/usuarios/{id}` | Actualizar |
| DELETE | `/usuarios/{id}?deactivate=true|false` | Soft-delete (true) o hard-delete (false) |

### Planteles (`/planteles`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/planteles` | Listar todos (paginado) |
| GET | `/planteles/{id}` | Obtener por ID |
| POST | `/planteles` | Crear (201) |
| PUT | `/planteles/{id}` | Actualizar |
| DELETE | `/planteles/{id}` | Soft-delete (204) |

### Alumnos (`/alumnos`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/alumnos` | Listar todos (paginado) |
| GET | `/alumnos/{id}` | Obtener por ID |
| POST | `/alumnos` | Crear (201). Valida CURP, correo únicos y estatus FK existente. |
| PUT | `/alumnos/{id}` | Actualizar |
| DELETE | `/alumnos/{id}` | Soft-delete (204) |

### Grupos (`/grupos`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/grupos` | Listar todos (paginado) |
| GET | `/grupos/{id}` | Obtener por ID |
| GET | `/grupos/{id}/cuatrimestres` | Cantidad de cuatrimestres de la carrera |
| GET | `/grupos/{id}/cuatrimestres/{cuatrimestre}/materias` | Materias de un cuatrimestre |
| GET | `/grupos/{id}/alumnos` | Alumnos asignados al grupo (paginado, `?search=`) |
| GET | `/grupos/{id}/alumnos-disponibles` | Alumnos no asignados al grupo (paginado, `?search=`) |
| POST | `/grupos` | Crear (201). Clave auto-generada si no se provee. |
| PUT | `/grupos/{id}` | Actualizar |
| DELETE | `/grupos/{id}` | Soft-delete (204) |

### Alumnos-Grupos (`/alumnos-grupos`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/alumnos-grupos` | Listar asignaciones (paginado) |
| GET | `/alumnos-grupos/{id}` | Obtener por ID |
| POST | `/alumnos-grupos` | Asignar alumno a grupo (201). Unique: alumno+grupo. |
| PUT | `/alumnos-grupos/{id}` | Actualizar |
| DELETE | `/alumnos-grupos/{id}` | Soft-delete (204). Sin updated_at. |

### Profesores-Grupos (`/profesores-grupos`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/profesores-grupos` | Listar asignaciones con nombres (profesor/grupo/materia) — paginado, `?search=` (ILIKE sobre profesor, grupo y materia) |
| GET | `/profesores-grupos/{id}` | Obtener por ID |
| POST | `/profesores-grupos` | Asignar profesor a grupo+materia (201). Unique: profesor+grupo+materia. |
| PUT | `/profesores-grupos/{id}` | Actualizar |
| DELETE | `/profesores-grupos/{id}` | Soft-delete (204). Sin updated_at. |

### Calificaciones (`/calificaciones`)

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/calificaciones` | Listar todas (paginado) |
| GET | `/calificaciones?alumnoId=X` | Calificaciones de un alumno (sin paginación) |
| GET | `/calificaciones/{id}` | Obtener por ID |
| POST | `/calificaciones` | Registrar calificación (201). Rango 0-100. El alumno debe estar asignado al grupo. Unique: alumno+materia+grupo. |
| PUT | `/calificaciones/{id}` | Actualizar |
| DELETE | `/calificaciones/{id}` | Soft-delete (204) |

> **Validación alumno↔grupo:** `CalificacionService.validateEnrollment(alumnoId, grupoId)` exige una fila activa en `alumnos_grupos`. Reforzado en BD con la FK compuesta `fk_calificaciones_alumno_grupo (alumno_id, grupo_id) → alumnos_grupos (alumno_id, grupo_id)` (usa el unique `uq_alumno_grupo`).

### Auth (`/auth`)

| Método | Ruta | Acción |
|--------|------|--------|
| POST | `/auth/login` | Login, devuelve JWT |

## Seguridad y JWT

- `JwtAuthenticationFilter` extiende `OncePerRequestFilter`. Procesa el header `Authorization: Bearer <token>`.
- **No tiene `shouldNotFilter`** (no se excluye `/auth/**` del filtro JWT). Si hay un token expirado en el header al hacer login, `jwtService.extractEmail()` lanza `ExpiredJwtException` y da 403 — el login nunca llega al controller.
- Roles en DB: `admin`, `rector`, `docente`, `servicios_escolares`, `coordinador`. Spring Security usa `.hasRole("ADMIN")` (añade prefijo `ROLE_` automáticamente).
- Token JWT incluye `rol` como claim (minúsculas, sin prefijo).
- `UsuarioDetailsService.loadUserByUsername(email)` se ejecuta en **cada request autenticado** (lo llama `JwtAuthenticationFilter`). Es la consulta más frecuente a la BD.

### CORS
- Origen: `http://localhost:4200`, métodos: GET/POST/PUT/DELETE/OPTIONS, `allowCredentials = true`

## Manejo de errores

- `ResourceNotFoundException` → 404 | `IllegalArgumentException` → 400 | `BadCredentialsException`/`UsernameNotFoundException` → 401
- Todo centralizado en `GlobalExceptionHandler`; controllers no hacen try/catch.

## Paginación

Todos los endpoints GET que retornan listas (`getAll`) están paginados. Los endpoints que retornan una sola entidad (`getById`, `findByAlumnoId`, `getMateriasByCuatrimestre`, etc.) **no** llevan paginación.

### Parámetros de query

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `page` | `0` | Página actual (0-indexed) |
| `size` | `20` | Elementos por página (1-100) |

Ejemplo: `GET /alumnos?page=0&size=10`

### PageResponse DTO

```java
// mx.cucii.school.platform.dto.PageResponse
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
```

### Ejemplo de respuesta

```json
{
  "content": [
    { "id": 1, "nombres": "Juan", "primerApellido": "Pérez", ... }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

### Endpoints paginados

| Ruta | Controlador | Servicio |
|------|------------|----------|
| `GET /alumnos` | `AlumnoController.getAll(page, size)` | `AlumnoService.findAll(page, size)` |
| `GET /alumnos-grupos` | `AlumnoGrupoController.getAll(page, size)` | `AlumnoGrupoService.findAll(page, size)` |
| `GET /calificaciones` (sin alumnoId) | `CalificacionController.getAll(page, size)` | `CalificacionService.findAll(page, size)` |
| `GET /grupos` | `GrupoController.getAll(page, size)` | `GrupoService.findAll(page, size)` |
| `GET /materias` | `MateriaController.getAll(page, size)` | `MateriaService.findAll(page, size)` |
| `GET /planes-estudio` | `PlanEstudioController.getAll(page, size)` | `PlanEstudioService.findAll(page, size)` |
| `GET /planes-estudio/con-materias-count` | `PlanEstudioController.getAllWithMateriasCount(page, size)` | `PlanEstudioService.findAllWithMateriasCount(page, size)` |
| `GET /planteles` | `PlantelController.getAll(page, size)` | `PlantelService.findAll(page, size)` |
| `GET /profesores-grupos` | `ProfesorGrupoController.getAll(page, size)` | `ProfesorGrupoService.findAll(page, size)` |
| `GET /usuarios` | `UsuarioController.getAll(page, size)` | `UsuarioService.findAll(page, size)` |

### Endpoints EXCLUIDOS de paginación (retornan lista completa)

| Ruta | Motivo |
|------|--------|
| `GET /calificaciones?alumnoId=X` | Se necesitan todas las calificaciones de un alumno de golpe |
| `GET /grupos/{id}/cuatrimestres/{n}/materias` | Se necesitan todas las materias de un cuatrimestre de la carrera |

### Patrón en el service

```java
public PageResponse<XxxResponse> findAll(int page, int size) {
    if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
    if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
    if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

    long totalElements = repository.countAll();
    int totalPages = (int) Math.ceil((double) totalElements / size);
    int offset = page * size;

    List<XxxResponse> content = repository.findAll(size, offset).stream()
            .map(this::toResponse)
            .toList();

    return new PageResponse<>(content, totalElements, totalPages, page, size);
}
```

### Patrón en el controller

```java
@GetMapping
public ResponseEntity<PageResponse<XxxResponse>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(service.findAll(page, size));
}
```

## Respuestas HTTP

- POST → 201 Created con body | DELETE → 204 No Content sin body | GET/PUT → 200 OK con body

## Base de datos

- Puerto 5432, usuario `ssant0`, DB `ss-platform`, password `2004`
- Esquema completo: `../database/db_structure.sql`
- Seed data con passwords bcrypt en `application.properties`. **La contraseña de prueba es `Test1234!`** (no `1234` como aparece en los mocks del frontend).
- Email del admin: `admin@cucii.edu.mx`

## Config (`src/main/resources/application.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ss-platform
security.jwt.secret=cucii-platform-super-secret-key-change-in-production-2024
security.jwt.expiration=8640000000   # 100 días (dev)
```

## Rutas actuales en SecurityConfig

| Patrón | Acceso |
|--------|--------|
| `/auth/**` | público |
| `/usuarios/**` | `ADMIN` |
| `/planes-estudio/**` | `ADMIN` |
| `/materias/**` | `ADMIN` |
| `/planteles/**` | `ADMIN` |
| `/alumnos/**` | `ADMIN` |
| `/grupos/**` | `ADMIN` |
| `/alumnos-grupos/**` | `ADMIN` |
| `/profesores-grupos/**` | `ADMIN` |
| `/calificaciones/**` | `ADMIN` |
| cualquier otra | JWT válido |

## Rama de migración JdbcTemplate

Toda la migración de Spring Data JDBC → JdbcTemplate se realizó en la rama `refactor/jdbctemplate` con feature branches individuales:

| Rama | Scope |
|------|-------|
| `jdbc-plan-estudio` | PlanEstudioJdbcRepository + PlanEstudioService |
| `jdbc-materia` | MateriaJdbcRepository + MateriaService |
| `jdbc-usuario` | UsuarioJdbcRepository + RolJdbcRepository + UsuarioService |
| `jdbc-auth` | AuthService + UsuarioDetailsService migrados a los nuevos repos |
| `fix/lastname` | Agregar campo `apellido` a Usuario (model, DTOs, repository, service) |
| `jdbc-plantel` | PlantelJdbcRepository + PlantelService + PlantelController |
| `jdbc-alumno` | AlumnoJdbcRepository + AlumnoService + AlumnoController |
| `jdbc-grupo` | GrupoJdbcRepository + GrupoService + GrupoController |
| `jdbc-alumno-grupo` | AlumnoGrupoJdbcRepository + AlumnoGrupoService + AlumnoGrupoController |
| `jdbc-profesor-grupo` | ProfesorGrupoJdbcRepository + ProfesorGrupoService + ProfesorGrupoController |
| `jdbc-calificacion` | CalificacionJdbcRepository + CalificacionService + CalificacionController |

## Features implementadas

| Rama | Scope |
|------|-------|
| `db/estatus-alumno` | Tabla `estatus_alumnos` + columna `estatus_id` en `alumnos` (SQL) |
| `feature/estatus-alumno-api` | `EstatusAlumno` model + `EstatusAlumnoJdbcRepository` + validación FK en `AlumnoService` |

Convención para futuras migraciones: `jdbc-{entidad}`.
