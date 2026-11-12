# 📡 Gestión Universitaria CUCII — API REST

Backend de la plataforma de gestión académica CUCII. API REST construida con Spring Boot y JdbcTemplate, con autenticación stateless JWT.

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](../README.md#licencia)

---

## 📑 Tabla de Contenidos

- [Stack](#stack)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Inicio Rápido](#inicio-rápido)
- [API](#api)
- [Seguridad](#seguridad)
- [Validaciones](#validaciones)
- [Scripts](#scripts)

---

## Stack

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 | Lenguaje |
| Spring Boot | 4.0.6 | Framework web |
| Spring Security | 7 | Autenticación y autorización |
| JdbcTemplate | — | Acceso a BD con SQL explícito |
| PostgreSQL | 18 | Base de datos |
| Lombok | — | `@RequiredArgsConstructor` |
| JJWT | 0.12.6 | Generación y validación de JWT (HS256) |

> [!NOTE]
> No se usa JPA, Hibernate ni Spring Data JDBC. Todo el SQL es explícito mediante `JdbcTemplate` + `RowMapper`.

---

## Arquitectura

```
Controller (HTTP) → Service (lógica) → Repository (SQL) → PostgreSQL
```

```
src/main/java/mx/cucii/school/platform/
├── config/          SecurityFilterChain, CORS, PasswordEncoder, AuthenticationManager
├── controller/      @RestController — solo maneja HTTP, delega al service
├── dto/             Records request/response
├── exception/       ResourceNotFoundException, GlobalExceptionHandler
├── model/           Records inmutables (@Table, @Id, @Column como documentación)
├── repository/      @Repository con JdbcTemplate + RowMapper (sufijo *JdbcRepository)
├── security/        JwtAuthenticationFilter (OncePerRequestFilter)
└── service/         Lógica de negocio, validación, timestamps, mapeo a DTOs
```

### Reglas por capa

- **Controller:** Solo HTTP. Cero lógica de negocio o SQL.
- **Service:** Validación, timestamps con `OffsetDateTime.now()`, orquestación entre repos. Cero SQL.
- **Repository:** SQL explícito (`SELECT`, `INSERT ... RETURNING *`, `UPDATE ... RETURNING *`). Cero lógica de negocio.
- **Model:** Java records. Anotaciones `@Table`/`@Id`/`@Column` son redundantes con JdbcTemplate pero se mantienen como documentación.

---

## Requisitos

- JDK 21+
- Docker (para PostgreSQL)
- Maven (wrapper incluido: `./mvnw`)

---

## Inicio Rápido

```bash
# 1. Iniciar PostgreSQL
cd database && docker compose up -d

# 2. Iniciar servidor
./mvnw spring-boot:run

# 3. Probar autenticación (credenciales de desarrollo)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cucii.edu.mx","password":"Admin1234!Dev"}'
```

### Base de datos

| Parámetro | Valor |
|-----------|-------|
| Host | `localhost:5432` |
| Base de datos | `ss-platform` |
| Usuario | `ssant0` |
| Contraseña | `2004` |

Al crear el volumen por primera vez, PostgreSQL ejecuta `../database/db_structure.sql`
(esquema + catálogos) y `../database/init/02_bootstrap.sh` (plantel base, admin y
docente). Los datos demo opcionales están en `../database/demo/demo_data.sql`.

> En producción las credenciales y el bootstrap se controlan con `DB_*`,
> `ADMIN_*` y `TEACHER_*` desde `.env.prod`. Ver `../docs/DEPLOY.md`.

---

## API

- Desarrollo: `http://localhost:8080`
- Producción: `https://api.gestion.cucii.mx`

> [!IMPORTANT]
> `/auth/login` es público. El resto requiere `Authorization: Bearer <token>`.
> El rol `docente` puede consultar catálogos/alumnos/grupos y registrar o
> actualizar calificaciones. Los CRUDs administrativos y el borrado de
> calificaciones requieren `ADMIN`.

### Autenticación

```http
POST /auth/login
Content-Type: application/json

{"email": "admin@cucii.edu.mx", "password": "<ADMIN_PASSWORD>"}

# Response: 200
# { "token": "eyJ...", "id": 1, "nombre": "Administrador", "apellido": "Sistema",
#   "email": "admin@cucii.edu.mx", "rolId": 1 }
```

```http
GET /auth/me
Authorization: Bearer <token>

# Response: 200
# { "id": 1, "nombre": "Administrador", "apellido": "Sistema",
#   "email": "admin@cucii.edu.mx", "rolId": 1 }
```

### Catálogos

| Recurso | Endpoint | GET | GET/{id} | POST | PUT | DELETE |
|---------|----------|-----|----------|------|-----|--------|
| Planes de estudio | `/planes-estudio` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Materias | `/materias` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Usuarios | `/usuarios` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Planteles | `/planteles` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Alumnos | `/alumnos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Grupos | `/grupos` | ✅ | ✅ | ✅ | ✅ | ✅ |

### Relaciones

| Recurso | Endpoint | POST | PUT | DELETE |
|---------|----------|------|-----|--------|
| Alumno ↔ Grupo | `/alumnos-grupos` | Asignar | Actualizar | Soft-delete |
| Profesor ↔ Grupo ↔ Materia | `/profesores-grupos` | Asignar | Actualizar | Soft-delete |

> `GET /profesores-grupos` devuelve las asignaciones con los nombres de profesor, grupo y materia, y acepta `?search=` sobre esos campos.

### Calificaciones

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/calificaciones` | Listar todas |
| GET | `/calificaciones/{id}` | Obtener por ID |
| POST | `/calificaciones` | Registrar (rango 0-100) |
| PUT | `/calificaciones/{id}` | Actualizar |
| DELETE | `/calificaciones/{id}` | Soft-delete |

### Endpoints adicionales

```http
GET  /planes-estudio/{id}/con-materias           # Plan + materias activas (LEFT JOIN)
GET  /planes-estudio/{id}/con-materias-sql       # Alias
GET  /grupos/{id}/alumnos                        # Alumnos asignados (paginado, ?search=)
GET  /grupos/{id}/alumnos-disponibles            # Alumnos no asignados (paginado, ?search=)
POST /alumnos/{id}/restore                       # Reactivar alumno archivado
DEL  /usuarios/{id}?deactivate=true              # Soft-delete
DEL  /usuarios/{id}?deactivate=false             # Hard-delete
```

---

## Seguridad

- **JWT:** Header `Authorization: Bearer <token>`
- **Claims:** `sub` = email, `rol` = `rolId` numérico (1=admin, 2=rector, 3=docente, 4=servicios_escolares, 5=coordinador)
- **Roles en DB:** `admin`, `rector`, `docente`, `servicios_escolares`, `coordinador`
- **Filtro:** `JwtAuthenticationFilter` excluye `/auth/login`. Los tokens inválidos o expirados se capturan y la petición continúa sin autenticar.
- **Errores de seguridad:** `401` para no autenticado y `403` para sin permisos, ambos con JSON `{"error": "..."}`.

### Acceso por rol (beta)

| Rutas | Acceso |
|-------|--------|
| `/auth/login`, `/actuator/health`, Swagger | Público |
| GET `/planes-estudio/**`, `/materias/**`, `/planteles/**`, `/alumnos/**`, `/grupos/**`, `/alumnos-grupos/**`, `/profesores-grupos/**` | `ADMIN`, `DOCENTE` |
| GET/POST/PUT `/calificaciones/**` | `ADMIN`, `DOCENTE` |
| DELETE `/calificaciones/**` | `ADMIN` |
| `/usuarios/**` y escrituras de catálogos/CRUDs | `ADMIN` |

> El docente todavía no está restringido a sus propios grupos/materias. Esa
> matriz fina corresponde al hito **HG** de `docs/HITOS_PENDIENTES.md`.

---

## Validaciones

| Campo | Validación |
|-------|------------|
| `alumnos.curp` | Obligatorio, formato oficial CURP (regex en service + constraint en PostgreSQL) |
| `alumnos.correo_institucional` | Obligatorio, único y con formato de correo en el frontend |
| `alumnos.estatus_id` | Obligatorio y debe existir en `estatus_alumnos` |
| `grupos.clave` | Auto-generada como `CG-{n}` vía secuencia si no se provee |
| `calificaciones.calificacion` | Rango 0-100 (BigDecimal) |
| `calificaciones.alumno + grupo` | El alumno debe estar asignado (activo) al grupo; validado en service + FK compuesta en PostgreSQL |
| `planteles.pais` | Default `'México'` si no se especifica |
| Unique `alumno + grupo` | No duplicar asignación en `alumnos_grupos` |
| Unique `profesor + grupo + materia` | No duplicar asignación en `profesores_grupos` |
| Unique `alumno + materia + grupo` | Una sola calificación por alumno/materia/grupo |

---

## Scripts

```bash
./mvnw spring-boot:run           # Servidor de desarrollo (DevTools, hot-reload)
./mvnw test                      # Todos los tests
./mvnw test -Dtest=NombreTest   # Test específico
./mvnw compile                   # Compilar sin ejecutar
./mvnw package -DskipTests       # Generar JAR en target/
./mvnw clean                     # Limpiar build
```

---

## Despliegue

El API se empaqueta con `server/Dockerfile` y se orquesta junto con PostgreSQL y
Caddy mediante `docker-compose.prod.yaml`.

```bash
cp deploy/.env.prod.example .env.prod
# completar secretos
docker compose --env-file .env.prod -f docker-compose.prod.yaml up -d --build
```

Guía completa: [`../docs/DEPLOY.md`](../docs/DEPLOY.md).
Hitos pendientes: [`../docs/HITOS_PENDIENTES.md`](../docs/HITOS_PENDIENTES.md).
