# 📡 CUCII Calificaciones — API REST

Backend de la plataforma de gestión académica CUCII. API REST construida con Spring Boot y JdbcTemplate, con autenticación stateless JWT.

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

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

# 3. Probar autenticación
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cucii.edu.mx","password":"Test1234!"}'
```

### Base de datos

| Parámetro | Valor |
|-----------|-------|
| Host | `localhost:5432` |
| Base de datos | `ss-platform` |
| Usuario | `ssant0` |
| Contraseña | `2004` |

El esquema se inicializa automáticamente desde `../database/db_structure.sql`.

---

## API

Base URL: `http://localhost:8080`

> [!IMPORTANT]
> Todas las rutas requieren rol `ADMIN` y header `Authorization: Bearer <token>`, excepto `/auth/login` que es público.

### Autenticación

```http
POST /auth/login
Content-Type: application/json

{"email": "admin@cucii.edu.mx", "password": "Test1234!"}

# Response: 200
# { "token": "eyJ...", "nombre": "Admin", "email": "admin@cucii.edu.mx", "rol": "admin" }
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
DEL  /usuarios/{id}?deactivate=true              # Soft-delete
DEL  /usuarios/{id}?deactivate=false             # Hard-delete
```

---

## Seguridad

- **JWT:** Header `Authorization: Bearer <token>`
- **Claims:** `sub` = email, `rol` = nombre del rol (minúsculas, sin prefijo)
- **Spring Security:** `.hasRole("ADMIN")` — añade prefijo `ROLE_` automáticamente
- **Roles en DB:** `admin`, `rector`, `docente`, `servicios_escolares`, `coordinador`
- **Filtro:** `JwtAuthenticationFilter` se ejecuta en cada request (no excluye `/auth/**`)

> [!WARNING]
> El filtro JWT se ejecuta también en `/auth/login`. Si el header contiene un token expirado, el login fallará con 403. Enviar la petición sin header para login.

---

## Validaciones

| Campo | Validación |
|-------|------------|
| `alumnos.curp` | Formato oficial CURP (regex en service + constraint en PostgreSQL) |
| `grupos.clave` | Auto-generada como `CG-{n}` vía secuencia si no se provee |
| `calificaciones.calificacion` | Rango 0-100 (BigDecimal) |
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
