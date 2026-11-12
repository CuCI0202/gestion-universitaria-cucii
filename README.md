# 🎓 Gestión Universitaria CUCII

Sistema integral de gestión académica para el **Centro Universitario de Ciencias de la Información (CUCII)**.

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21.2-%23DD0031?logo=angular)](https://angular.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](#licencia)

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
| **Frontend** | Angular 21.2 · TailwindCSS 4.2 · RxJS 7.8 · TypeScript 5.9 |
| **Build** | Maven (mvnw) · pnpm |

---

## Estructura del Repositorio

```
gestion-universitaria-cucii/
├── database/               # PostgreSQL — esquema + Docker Compose
│   ├── db_structure.sql    # Esquema + catálogos base (roles, estatus)
│   ├── init/               # Bootstrap de producción (admin/docente)
│   ├── demo/               # Datos demo opcionales para desarrollo
│   ├── docker-compose.yaml # Contenedor PostgreSQL 18 (desarrollo)
│   └── dbdoc/              # Documentación visual generada con tbls
├── deploy/                 # Caddyfile y plantilla .env.prod
├── docs/DEPLOY.md          # Guía de despliegue paso a paso
├── scripts/backup.sh       # Respaldo diario de PostgreSQL
├── server/                 # API REST — Spring Boot (Java 21)
│   ├── src/                # Código fuente
│   ├── Dockerfile          # Imagen de producción del API
│   ├── pom.xml             # Dependencias Maven
│   └── README.md           # Documentación detallada del backend
└── view/                   # SPA — Angular 21
    ├── src/                # Componentes, servicios, rutas
    ├── angular.json        # Configuración de Angular
    └── README.md           # Documentación detallada del frontend
```

> Guía de despliegue: [`docs/DEPLOY.md`](docs/DEPLOY.md).

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
cd gestion-universitaria-cucii
```

### 2. Iniciar base de datos

```bash
cd database
docker compose up -d
```

El contenedor `cucii-ss-platform` expone PostgreSQL en `localhost:5432`. Al iniciar por primera vez ejecuta `db_structure.sql` (esquema + catálogos) y `database/init/02_bootstrap.sh` (plantel base, administrador y usuario docente).

Si quieres datos demo para desarrollo, con un volumen limpio ejecuta además `database/demo/demo_data.sql` (sin el bootstrap).

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

El backend se configura con variables de entorno (ver `.env.example` para desarrollo y `deploy/.env.prod.example` para producción):

| Variable | Descripción | Default dev |
|----------|-------------|-------------|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring (`dev` o `prod`) | `dev` |
| `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/ss-platform` |
| `DB_USER` | Usuario de BD | `ssant0` |
| `DB_PASSWORD` | Contraseña de BD | `2004` |
| `JWT_SECRET` | Clave secreta para firmar JWT | valor de desarrollo |
| `JWT_EXPIRATION` | Expiración del token (ms) | `86400000` (24 h) |
| `CORS_ORIGINS` | Orígenes del frontend separados por coma | `http://localhost:4200` |

> [!WARNING]
> En producción usa `SPRING_PROFILES_ACTIVE=prod`; el perfil estricto falla si falta alguna variable. Nunca reutilices los valores de desarrollo.

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

### Credenciales de desarrollo

Las define `database/docker-compose.yaml` (o tu `.env`):

```
Admin:    admin@cucii.edu.mx   / Admin1234!Dev
Docente:  docente@cucii.edu.mx / Docente1234!Dev
```

En producción se definen con `ADMIN_EMAIL`/`ADMIN_PASSWORD` y
`TEACHER_EMAIL`/`TEACHER_PASSWORD` en `.env.prod`, y se crean solo con el
volumen vacío.

---

## API REST

- Desarrollo: `http://localhost:8080`
- Producción: `https://api.gestion.cucii.mx`

### Autenticación

```http
POST /auth/login
Content-Type: application/json

{"email": "admin@cucii.edu.mx", "password": "<ADMIN_PASSWORD>"}

# Response: 200
# { "token": "eyJ...", "id": 1, "nombre": "Administrador", "apellido": "Sistema",
#   "email": "admin@cucii.edu.mx", "rolId": 1 }
```

> [!IMPORTANT]
> Las rutas protegidas requieren el header `Authorization: Bearer <token>`.
> `/auth/login` es público; `/auth/me` requiere token.
> El rol `docente` puede consultar catálogos/alumnos/grupos y registrar o
> actualizar calificaciones. Los CRUDs administrativos siguen siendo solo `admin`.

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
| GET | `/planes-estudio/{id}/con-materias` | Plan + materias activas (LEFT JOIN) | ADMIN, DOCENTE |
| GET | `/planes-estudio/{id}/con-materias-sql` | Alias del anterior | ADMIN, DOCENTE |
| GET | `/grupos/{id}/alumnos` | Alumnos asignados al grupo (paginado, `?search=`) | ADMIN, DOCENTE |
| GET | `/grupos/{id}/alumnos-disponibles` | Alumnos no asignados al grupo (paginado, `?search=`) | ADMIN, DOCENTE |
| GET | `/profesores-grupos` | Asignaciones con nombres de profesor/grupo/materia (paginado, `?search=`) | ADMIN, DOCENTE |
| DELETE | `/usuarios/{id}?deactivate=true` | Soft-delete | ADMIN |
| DELETE | `/usuarios/{id}?deactivate=false` | Hard-delete | ADMIN |

---

## Roles del Sistema

| Rol | Descripción | Estado beta |
|-----|-------------|-------------|
| `admin` | Acceso total al sistema | Activo |
| `docente` | Consulta catálogos/alumnos/grupos y registra calificaciones | Activo |
| `rector` | Rector del plantel | Pendiente (matriz de permisos) |
| `servicios_escolares` | Responsable de servicios escolares | Pendiente |
| `coordinador` | Gestión de grupos y alumnos | Pendiente |

> En esta beta solo `admin` accede a los CRUDs administrativos. El `docente`
> puede consultar y registrar/actualizar calificaciones, pero no eliminarlas.

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
| `usuarios` | Personal con rol asignado y plantel obligatorio |
| `profesores_grupos` | Asignación profesor ↔ grupo ↔ materia |
| `calificaciones` | Calificaciones por alumno, materia y grupo |

Documentación visual (diagramas ER) en `database/dbdoc/` generada con [tbls](https://github.com/k1LoW/tbls).

---

## Pantallas del Frontend

| Ruta | Acceso | Descripción |
|------|--------|-------------|
| `/login` | Público | Inicio de sesión |
| `/browse` | Autenticado | Consulta de alumnos y calificaciones por cuatrimestre |
| `/upload` | Autenticado | Registro manual de calificaciones (CSV en la siguiente iteración) |
| `/students` | Admin | CRUD de alumnos |
| `/profesores` | Admin | Asignación profesor ↔ grupo ↔ materia |
| `/groups` | Admin | CRUD de grupos |
| `/programs` | Admin | CRUD de planes de estudio |
| `/subjects` | Admin | CRUD de materias por plan |
| `/campuses` | Admin | CRUD de planteles |
| `/users` | Admin | CRUD de usuarios del sistema |

---

## Despliegue

La guía completa está en [`docs/DEPLOY.md`](docs/DEPLOY.md). Resumen:

- **Frontend:** Cloudflare Pages (root `view`, build `pnpm install && pnpm build`,
  output `dist/calificaciones/browser`).
- **Backend + DB + HTTPS:** VPS con Docker Compose (`docker-compose.prod.yaml`)
  y Caddy como servicio Docker.
- **Variables:** `cp deploy/.env.prod.example .env.prod` y completar secretos.
- **Backups:** `scripts/backup.sh` + cron diario.
- **Base limpia:** catálogos (`roles`, `estatus_alumnos`) + plantel + admin +
  docente. Los datos académicos se capturan desde la app.

Hitos pendientes y orden recomendado: [`docs/HITOS_PENDIENTES.md`](docs/HITOS_PENDIENTES.md).

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
