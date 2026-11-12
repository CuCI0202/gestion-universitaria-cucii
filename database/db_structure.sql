-- =============================================================================
-- Esquema + catálogos base de Gestión Universitaria CUCII.
--
-- Este archivo NO crea usuarios ni datos académicos reales. El arranque de
-- producción usa database/init/02_bootstrap.sh para crear el plantel, el
-- administrador y el usuario docente desde variables de entorno.
--
-- Datos demo para desarrollo: database/demo/demo_data.sql (NO producción).
-- =============================================================================

-- ─── planteles ───────────────────────────────────────────────────────────────

create table planteles
(
    id                   integer generated always as identity primary key,
    nombre_oficial       varchar(150) not null,
    nombre_corto         varchar(50)  not null          default '',
    direccion_calle      varchar(100) not null          default '',
    direccion_numero_ext varchar(20)  not null          default '',
    direccion_numero_int varchar(20)                    default '',
    colonia              varchar(100) not null          default '',
    codigo_postal        varchar(10)  not null          default '',
    ciudad_municipio     varchar(100) not null          default '',
    estado               varchar(100) not null          default '',
    pais                 varchar(50)  not null          default 'México',
    latitud              numeric(10, 8),
    longitud             numeric(11, 8),
    director_nombre      varchar(150) not null          default '',
    is_active            boolean      not null          default true,
    created_at           timestamp with time zone       default current_timestamp not null,
    updated_at           timestamp with time zone       default current_timestamp not null
);


create index idx_planteles_ciudad on planteles (ciudad_municipio);
create index idx_planteles_estado on planteles (estado);

-- ─── planes_estudio ──────────────────────────────────────────────────────────

create table planes_estudio
(
    id                     integer generated always as identity primary key,
    nombre                 varchar(120)                                       not null,
    grado                  varchar(20)                                        not null,
    numero_rvoe            varchar(50)                                        not null unique,
    fecha_rvoe             date                                               not null,
    duracion_cuatrimestres integer                                            not null,
    is_active              boolean                  default true,
    created_at             timestamp with time zone default current_timestamp not null,
    updated_at             timestamp with time zone default current_timestamp not null,

    constraint chk_grado check (grado in ('Licenciatura', 'Maestría', 'Doctorado'))
);


-- ─── materias ────────────────────────────────────────────────────────────────

create table materias
(
    id              integer generated always as identity primary key,
    nombre          varchar(120)                                       not null,
    clave           varchar(20)                                        not null,
    creditos        numeric(5, 2)                                      not null,
    cuatrimestre    integer                                            not null,
    plan_estudio_id integer                                            not null,
    is_active       boolean                                            not null default true,
    created_at      timestamp with time zone default current_timestamp not null,
    updated_at      timestamp with time zone default current_timestamp not null,

    constraint fk_materias_plan_estudio
        foreign key (plan_estudio_id)
            references planes_estudio (id)
            on update cascade on delete cascade
);


create index idx_materias_plan_estudio on materias (plan_estudio_id);

-- ─── estatus_alumnos ─────────────────────────────────────────────────────────

create table estatus_alumnos
(
    id          integer generated always as identity primary key,
    nombre      varchar(50)  not null unique,
    descripcion varchar(200) not null,
    is_active   boolean      not null default true,
    created_at  timestamp with time zone default current_timestamp not null,
    updated_at  timestamp with time zone default current_timestamp not null
);


insert into estatus_alumnos (nombre, descripcion)
values ('Invasión', 'Alumno en invasión de ciclos'),
       ('Cursando', 'Alumno cursando activamente'),
       ('Egresado', 'Alumno egresado'),
       ('Baja', 'Alumno dado de baja');

-- ─── alumnos ─────────────────────────────────────────────────────────────────

create table alumnos
(
    id                   integer generated always as identity primary key,
    nombres              varchar(80)                                        not null,
    primer_apellido      varchar(80)                                        not null,
    segundo_apellido     varchar(80)              default '',
    curp                 char(18)                                           not null unique,
    correo_institucional varchar(120)                                       not null unique,
    estatus_id           integer                                            not null,
    is_active            boolean                                            not null default true,
    created_at           timestamp with time zone default current_timestamp not null,
    updated_at           timestamp with time zone default current_timestamp not null,

    constraint chk_curp_length check (char_length(curp) = 18),
    constraint chk_curp_format check (
        curp ~ '^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|1[0-9]|2[0-9]|3[0-1])[HMX][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$'
    ),

    constraint fk_alumnos_estatus
        foreign key (estatus_id)
            references estatus_alumnos (id)
            on update cascade on delete restrict
);


create index idx_alumnos_correo on alumnos (correo_institucional);

-- ─── roles ───────────────────────────────────────────────────────────────────

create table roles
(
    id          integer generated always as identity primary key,
    nombre      varchar(50)  not null unique,
    descripcion varchar(200) not null,
    is_active   boolean      not null    default true,
    created_at  timestamp with time zone default current_timestamp not null
);


insert into roles (nombre, descripcion)
values ('admin', 'Administrador del sistema con acceso total'),
       ('rector', 'Rector del plantel'),
       ('docente', 'Docente / profesor'),
       ('servicios_escolares', 'Responsable de servicios escolares'),
       ('coordinador', 'Responsable de coordinar grupos y gestión de alumnos');

-- ─── usuarios ────────────────────────────────────────────────────────────────

create table usuarios
(
    id            integer generated always as identity primary key,
    nombre        varchar(100)                                       not null,
    apellido      varchar(100)                                       not null,
    email         varchar(150)                                       not null unique,
    password_hash varchar(255)                                       not null,
    rol_id        integer                                            not null,
    plantel_id    integer                                            not null,
    is_active     boolean                                            not null default true,
    created_at    timestamp with time zone default current_timestamp not null,
    updated_at    timestamp with time zone default current_timestamp not null,

    constraint fk_usuarios_rol
        foreign key (rol_id)
            references roles (id)
            on update cascade on delete restrict,

    constraint fk_usuarios_plantel
        foreign key (plantel_id)
            references planteles (id)
            on update cascade on delete restrict
);


create index idx_usuarios_email on usuarios (email);
create index idx_usuarios_rol on usuarios (rol_id);
create index idx_usuarios_plantel on usuarios (plantel_id);

-- ─── grupos ──────────────────────────────────────────────────────────────────

create sequence grupos_numero_seq start 1;

create table grupos
(
    id              integer generated always as identity primary key,
    clave           varchar(20)  not null unique      default ('CG-' || nextval('grupos_numero_seq')::text),
    nombre          varchar(120) not null,
    plan_estudio_id integer      not null,
    plantel_id      integer      not null,
    is_active       boolean      not null    default true,
    created_at      timestamp with time zone default current_timestamp not null,
    updated_at      timestamp with time zone default current_timestamp not null,

    constraint fk_grupos_plan_estudio
        foreign key (plan_estudio_id)
            references planes_estudio (id)
            on update cascade on delete restrict,

    constraint fk_grupos_plantel
        foreign key (plantel_id)
            references planteles (id)
            on update cascade on delete restrict
);


create index idx_grupos_plan_estudio on grupos (plan_estudio_id);
create index idx_grupos_plantel on grupos (plantel_id);

-- ─── alumnos_grupos ──────────────────────────────────────────────────────────

create table alumnos_grupos
(
    id         integer generated always as identity primary key,
    alumno_id  integer not null,
    grupo_id   integer not null,
    is_active  boolean not null         default true,
    created_at timestamp with time zone default current_timestamp not null,

    constraint uq_alumno_grupo unique (alumno_id, grupo_id),

    constraint fk_alumnos_grupos_alumno
        foreign key (alumno_id)
            references alumnos (id)
            on update cascade on delete cascade,

    constraint fk_alumnos_grupos_grupo
        foreign key (grupo_id)
            references grupos (id)
            on update cascade on delete restrict
);


create index idx_alumnos_grupos_alumno on alumnos_grupos (alumno_id);
create index idx_alumnos_grupos_grupo on alumnos_grupos (grupo_id);

-- ─── profesores_grupos ────────────────────────────────────────────────────────

create table profesores_grupos
(
    id         integer generated always as identity primary key,
    usuario_id integer not null,
    grupo_id   integer not null,
    materia_id integer not null,
    is_active  boolean not null         default true,
    created_at timestamp with time zone default current_timestamp not null,

    constraint uq_profesor_grupo_materia unique (usuario_id, grupo_id, materia_id),

    constraint fk_profesores_grupos_usuario
        foreign key (usuario_id)
            references usuarios (id)
            on update cascade on delete restrict,

    constraint fk_profesores_grupos_grupo
        foreign key (grupo_id)
            references grupos (id)
            on update cascade on delete restrict,

    constraint fk_profesores_grupos_materia
        foreign key (materia_id)
            references materias (id)
            on update cascade on delete restrict
);


create index idx_profesores_grupos_usuario on profesores_grupos (usuario_id);
create index idx_profesores_grupos_grupo on profesores_grupos (grupo_id);
create index idx_profesores_grupos_materia on profesores_grupos (materia_id);

-- ─── calificaciones ──────────────────────────────────────────────────────────

create table calificaciones
(
    id             integer generated always as identity primary key,
    alumno_id      integer                                            not null,
    grupo_id       integer                                            not null,
    materia_id     integer                                            not null,
    calificacion   numeric(5, 2)                                      not null,
    registrado_por integer                                            not null,
    is_active      boolean                                            not null default true,
    created_at     timestamp with time zone default current_timestamp not null,
    updated_at     timestamp with time zone default current_timestamp not null,

    constraint chk_calificacion check (calificacion >= 0 and calificacion <= 100),

    constraint uq_alumno_materia_grupo unique (alumno_id, materia_id, grupo_id),

    constraint fk_calificaciones_alumno
        foreign key (alumno_id)
            references alumnos (id)
            on update cascade on delete cascade,

    constraint fk_calificaciones_alumno_grupo
        foreign key (alumno_id, grupo_id)
            references alumnos_grupos (alumno_id, grupo_id)
            on update cascade on delete cascade,

    constraint fk_calificaciones_grupo
        foreign key (grupo_id)
            references grupos (id)
            on update cascade on delete restrict,

    constraint fk_calificaciones_materia
        foreign key (materia_id)
            references materias (id)
            on update cascade on delete restrict,

    constraint fk_calificaciones_registrado_por
        foreign key (registrado_por)
            references usuarios (id)
            on update cascade on delete set null
);


create index idx_calificaciones_alumno on calificaciones (alumno_id);
create index idx_calificaciones_grupo on calificaciones (grupo_id);
create index idx_calificaciones_materia on calificaciones (materia_id);

