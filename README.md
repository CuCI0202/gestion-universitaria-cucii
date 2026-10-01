# 📊 Plataforma de Calificaciones CUCII

Sistema integral de gestión académica para el **Centro Universitario de Ciencias de la Información (CUCII)**.

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-20.3-%23DD0031?logo=angular)](https://angular.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## 📋 Descripción

Plataforma que permite registrar alumnos, grupos, planes de estudio, materias y calificaciones, con control de acceso basado en roles. Diseñada para instituciones educativas que necesitan gestionar su información académica de forma centralizada.

> [!NOTE]
> Este repositorio es un **monorepo** que contiene backend (API REST), frontend (SPA) y base de datos PostgreSQL.

---

## 📑 Tabla de Contenidos

- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Repositorio](#estructura-del-repositorio)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Variables de Entorno](#variables-de-entorno)
- [Uso y Desarrollo](#uso-y-desarrollo)
- [API REST](#api-rest)
- [Roles del Sistema](#roles-del-sistema)
- [Base de Datos](#base-de-datos)
- [Pantallas del Frontend](#pantallas-del-frontend)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Base de datos** | PostgreSQL 18 (Docker) |
| **Backend** | Java 21 · Spring Boot 4.0.6 · JdbcTemplate · Spring Security 7 · JJWT 0.12.6 · Lombok |
| **Frontend** | Angular 20.3 · TailwindCSS 4.2 · RxJS 7.8 · TypeScript 5.9 |
| **Build** | Maven (mvnw) · pnpm |

---

## Estructura del Repositorio

```
calificaciones.cucii.mx/
├── database/               # PostgreSQL — esquema + Docker Compose
│   ├── db_structure.sql    # Definición completa de tablas + seed data
│   ├── docker-compose.yaml # Contenedor PostgreSQL 18
│   └── dbdoc/              # Documentación visual generada con tbls
├── server/                 # API REST — Spring Boot (Java 21)
│   ├── src/                # Código fuente
│   ├── pom.xml             # Dependencias Maven
│   └── README.md           # Documentación detallada del backend
└── view/                   # SPA — Angular 20
    ├── src/                # Componentes, servicios, rutas
    ├── angular.json        # Configuración de Angular
    └── README.md           # Documentación detallada del frontend
```

---

## Requisitos Previos

> [!IMPORTANT]
> Las versiones indicadas son las mínimas requeridas.

| Herramienta | Versión | Verificación |
|------------|---------|-------------|
| Docker | >= 24 | `docker --version` |
| Java | >= 21 | `java --version` |
| Node.js | >= 20 | `node --version` |
| pnpm | >= 9 | `pnpm --version` |
| Maven | (wrapper incluido) | `./mvnw --version` |

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone <repo-url>
cd calificaciones.cucii.mx
```

### 2. Iniciar base de datos

```bash
cd database
docker compose up -d
```

El contenedor `cucii-ss-platform` expone PostgreSQL en `localhost:5432`. Al iniciar por primera vez ejecuta `db_structure.sql` automáticamente (tablas + datos de prueba).

### 3. Iniciar backend

```bash
cd server
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### 4. Iniciar frontend

```bash
cd view
pnpm install
pnpm start
```

El frontend se sirve en `http://localhost:4200`.

---

## Variables de Entorno

El backend se configura en `server/src/main/resources/application.properties`:

| Variable | Descripción | Default | Requerido |
|----------|-------------|---------|-----------|
| `spring.datasource.url` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/ss-platform` | Sí |
| `spring.datasource.username` | Usuario de BD | `ssant0` | Sí |
| `spring.datasource.password` | Contraseña de BD | `2004` | Sí |
| `security.jwt.secret` | Clave secreta para firmar JWT | `cucii-platform-super-secret-key-...` | Sí |
| `security.jwt.expiration` | Tiempo de expiración del token (ms) | `8640000000` (100 días) | Sí |

> [!WARNING]
> La clave JWT en `application.properties` es para desarrollo. En producción debe cambiarse por una clave segura y gestionarse como secreto de entorno.

---

## Uso y Desarrollo

### Backend

```bash
cd server

./mvnw spring-boot:run           # Servidor de desarrollo (hot-reload con DevTools)
./mvnw test                      # Ejecutar todos los tests
./mvnw test -Dtest=NombreTest    # Test específico
./mvnw package -DskipTests       # Generar JAR de producción
```

### Frontend

```bash
cd view

pnpm start                        # Servidor de desarrollo (http://localhost:4200)
ng build                          # Build de producción
ng build --configuration development  # Build rápido (~1.2s)
```

### Base de datos

```bash
cd database

docker compose up -d              # Iniciar PostgreSQL
docker compose down               # Detener contenedor
tbls doc                          # Regenerar documentación de BD
```

### Credenciales de prueba

```
Email:    admin@cucii.edu.mx
Password: Test1234!
```

---

## API REST

Base URL: `http://localhost:8080`

### Autenticación

```http
POST /auth/login
Content-Type: application/json

{"email": "admin@cucii.edu.mx", "password": "Test1234!"}

# Response: 200
# { "token": "eyJ...", "nombre": "Admin", "email": "admin@cucii.edu.mx", "rol": "admin" }
```

> [!IMPORTANT]
> Las rutas protegidas requieren el header `Authorization: Bearer <token>`.

### Endpoints por recurso

| Recurso | Endpoint | GET | GET/{id} | POST | PUT | DELETE |
|---------|----------|-----|----------|------|-----|--------|
| Planes de estudio | `/planes-estudio` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Materias | `/materias` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Usuarios | `/usuarios` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Planteles | `/planteles` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Alumnos | `/alumnos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Grupos | `/grupos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Alumno ↔ Grupo | `/alumnos-grupos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Profesor ↔ Grupo ↔ Materia | `/profesores-grupos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Calificaciones | `/calificaciones` | ✅ | ✅ | ✅ | ✅ | ✅ |

### Endpoints adicionales

| Método | Ruta | Descripción | Acceso |
|--------|------|-------------|--------|
| GET | `/planes-estudio/{id}/con-materias` | Plan + materias activas (LEFT JOIN) | ADMIN |
| GET | `/planes-estudio/{id}/con-materias-sql` | Alias del anterior | ADMIN |
| GET | `/grupos/{id}/alumnos` | Alumnos asignados al grupo (paginado, `?search=`) | ADMIN |
| GET | `/grupos/{id}/alumnos-disponibles` | Alumnos no asignados al grupo (paginado, `?search=`) | ADMIN |
| GET | `/profesores-grupos` | Asignaciones con nombres de profesor/grupo/materia (paginado, `?search=`) | ADMIN |
| DELETE | `/usuarios/{id}?deactivate=true` | Soft-delete | ADMIN |
| DELETE | `/usuarios/{id}?deactivate=false` | Hard-delete | ADMIN |

---

## Roles del Sistema

| Rol | Descripción |
|-----|-------------|
| `admin` | Acceso total al sistema |
| `rector` | Rector del plantel |
| `docente` | Profesor — puede registrar calificaciones |
| `servicios_escolares` | Responsable de servicios escolares |
| `coordinador` | Gestión de grupos y alumnos |

---

## Base de Datos

El esquema completo está en `database/db_structure.sql`. Tablas principales:

| Tabla | Descripción |
|-------|-------------|
| `planteles` | Sedes físicas de la institución |
| `planes_estudio` | Carreras (Licenciatura, Maestría, Doctorado) con RVOE |
| `materias` | Materias vinculadas a un plan de estudio |
| `alumnos` | Estudiantes con CURP único |
| `grupos` | Grupos por plan de estudio y plantel |
| `alumnos_grupos` | Relación alumno ↔ grupo |
| `roles` | Catálogo de roles del sistema |
| `usuarios` | Personal con rol asignado y plantel opcional |
| `profesores_grupos` | Asignación profesor ↔ grupo ↔ materia |
| `calificaciones` | Calificaciones por alumno, materia y grupo |

Documentación visual (diagramas ER) en `database/dbdoc/` generada con [tbls](https://github.com/k1LoW/tbls).

---

## Pantallas del Frontend

| Ruta | Acceso | Descripción |
|------|--------|-------------|
| `/login` | Público | Inicio de sesión |
| `/browse` | Autenticado | Consulta de alumnos y calificaciones por cuatrimestre |
| `/upload` | Autenticado | Registro manual o por CSV de calificaciones |
| `/students` | Admin | CRUD de alumnos |
| `/groups` | Admin | CRUD de grupos |
| `/programs` | Admin | CRUD de planes de estudio |
| `/subjects` | Admin | CRUD de materias por plan |
| `/campuses` | Admin | CRUD de planteles |
| `/users` | Admin | CRUD de usuarios del sistema |

---

## Contribuir

### Convención de ramas

| Prefijo | Propósito |
|---------|-----------|
| `jdbc-{entidad}` | Migración a JdbcTemplate |
| `fix/{descripcion}` | Corrección de errores |
| `feat/{descripcion}` | Nueva funcionalidad |

### Commits

Usar [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add CRUD for alumnos
fix: add apellido field to Usuario model
refactor: migrate Materia from Spring Data JDBC to JdbcTemplate
chore: remove unused Spring Data JDBC repositories
```

### Flujo

1. Crear rama desde `develop`
2. Implementar cambios
3. Merge a `develop` con `--no-ff`
4. Al finalizar iteración, merge a `main`

---

## Licencia

MIT
