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

alter table planteles
    owner to ssant0;

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

alter table planes_estudio
    owner to ssant0;

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

alter table materias
    owner to ssant0;

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

alter table estatus_alumnos
    owner to ssant0;

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

alter table alumnos
    owner to ssant0;

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

alter table roles
    owner to ssant0;

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

alter table usuarios
    owner to ssant0;

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

alter table grupos
    owner to ssant0;

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

alter table alumnos_grupos
    owner to ssant0;

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

alter table profesores_grupos
    owner to ssant0;

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

alter table calificaciones
    owner to ssant0;

create index idx_calificaciones_alumno on calificaciones (alumno_id);
create index idx_calificaciones_grupo on calificaciones (grupo_id);
create index idx_calificaciones_materia on calificaciones (materia_id);

-- =============================================================================
-- DATOS DE PRUEBA (40 estudiantes, 4 grupos, 8 docentes, 1 plantel)
-- Asume DB limpia: los IDs generados arrancan en 1 por tabla.
-- password_hash corresponde a la contraseña 'Test1234!' (bcrypt 10 rondas).
-- =============================================================================

-- ─── planteles (id: 1) ───────────────────────────────────────────────────────
insert into planteles (nombre_oficial, nombre_corto, direccion_calle, direccion_numero_ext, colonia, codigo_postal, ciudad_municipio, estado, latitud, longitud, director_nombre)
values ('Centro Universitario de Ciencias de la Información', 'CUCII', 'Av. Revolución', '1234', 'Centro', '44100', 'Guadalajara', 'Jalisco', 20.657699, -103.349400, 'Dr. Arturo Vega Ramírez');

-- ─── planes_estudio (id: 1-4) ────────────────────────────────────────────────
insert into planes_estudio (nombre, grado, numero_rvoe, fecha_rvoe, duracion_cuatrimestres)
values ('Ingeniería en Desarrollo de Software', 'Licenciatura', 'RVOE-2021-001', '2021-06-15', 12),
       ('Maestría en Ciencias de la Información', 'Maestría', 'RVOE-2022-002', '2022-01-20', 6),
       ('Maestría en Administración de Tecnologías de Información', 'Maestría', 'RVOE-2022-003', '2022-03-10', 6),
       ('Doctorado en Ciencias de la Información', 'Doctorado', 'RVOE-2023-001', '2023-05-12', 10);

-- ─── materias (plan 1 → ids 1-4 | plan 2 → ids 5-8 | plan 3 → ids 9-12 | plan 4 → ids 13-16) ──
insert into materias (nombre, clave, creditos, cuatrimestre, plan_estudio_id)
values ('Fundamentos de Programación', 'FP-101', 8, 1, 1),
       ('Matemáticas Discretas', 'MD-101', 6, 1, 1),
       ('Bases de Datos I', 'BD-201', 8, 2, 1),
       ('Ingeniería de Software', 'IS-301', 8, 3, 1),
       ('Metodología de la Investigación', 'MCI-101', 8, 1, 2),
       ('Estadística Avanzada', 'MCI-102', 6, 1, 2),
       ('Minería de Datos', 'MCI-201', 8, 2, 2),
       ('Aprendizaje Automático', 'MCI-301', 8, 3, 2),
       ('Gestión Estratégica de TI', 'MAT-101', 8, 1, 3),
       ('Arquitectura Empresarial', 'MAT-102', 6, 1, 3),
       ('Gobierno de TI', 'MAT-201', 8, 2, 3),
       ('Ciberseguridad', 'MAT-301', 8, 3, 3),
       ('Epistemología de la Información', 'DCI-101', 8, 1, 4),
       ('Seminario de Investigación I', 'DCI-102', 6, 1, 4),
       ('Seminario de Investigación II', 'DCI-201', 8, 2, 4),
       ('Tesis Doctoral I', 'DCI-301', 8, 3, 4);

-- ─── usuarios (id: 1 admin | 2 rector | 3-10 docentes | 11 servicios | 12 coordinador) ────
-- roles: 1=admin 2=rector 3=docente 4=servicios_escolares 5=coordinador
insert into usuarios (nombre, apellido, email, password_hash, rol_id, plantel_id)
values ('Admin', 'Sistema', 'admin@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 1, 1),
       ('Marco', 'Herrera', 'mherrera@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 2, 1),
       ('Ana', 'Torres', 'ana.torres@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Luis', 'Pérez', 'luis.perez@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Elena', 'Martínez', 'elena.martinez@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Roberto', 'Sánchez', 'roberto.sanchez@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Laura', 'Castillo', 'laura.castillo@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Jorge', 'Flores', 'jorge.flores@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Patricia', 'Ríos', 'patricia.rios@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Fernando', 'Díaz', 'fernando.diaz@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 3, 1),
       ('Carmen', 'Salinas', 'csalinas@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 4, 1),
       ('David', 'Ortiz', 'dortiz@cucii.edu.mx', '$2a$10$ah38fS7voJjmL7CIQTmEhu7.ULXxbZzItC2XTQtQiIRimmD/7x7iG', 5, 1);

-- ─── grupos (clave generada por secuencia: CG-1 … CG-4) ──────────────────────
insert into grupos (nombre, plan_estudio_id, plantel_id)
values ('IDS-2024A — Primer Cuatrimestre', 1, 1),
       ('MCI-2024A — Primer Cuatrimestre', 2, 1),
       ('MAT-2024A — Primer Cuatrimestre', 3, 1),
       ('DCI-2024A — Primer Cuatrimestre', 4, 1);

-- ─── alumnos (id: 1-40, 10 por grupo) ────────────────────────────────────────
insert into alumnos (nombres, primer_apellido, segundo_apellido, curp, correo_institucional, estatus_id)
values ('Juan', 'García', 'López', 'GALJ950320HJCRPNU1', 'juan.garcia@alumnos.cucii.edu.mx', 2),
       ('María', 'Rodríguez', 'Hernández', 'ROHM980705MJCDRRA4', 'maria.rodriguez@alumnos.cucii.edu.mx', 2),
       ('Carlos', 'Mendoza', 'Torres', 'METC001128HJCNRRH3', 'carlos.mendoza@alumnos.cucii.edu.mx', 2),
       ('Ana Patricia', 'Silva', 'Rivas', 'SIRA010203MJCLVNE1', 'ana.silva@alumnos.cucii.edu.mx', 2),
       ('Luis Fernando', 'Vega', 'Cruz', 'VECL020405HJCGZSV8', 'luis.vega@alumnos.cucii.edu.mx', 2),
       ('Gabriela', 'Núñez', 'Ortiz', 'NUOG030506MJCNRBC9', 'gabriela.nunez@alumnos.cucii.edu.mx', 2),
       ('Ricardo', 'Castillo', 'Mora', 'CAMR040607HJCSRCN0', 'ricardo.castillo@alumnos.cucii.edu.mx', 2),
       ('Sofía', 'Delgado', 'Ramos', 'DERS030708MJCLMFA1', 'sofia.delgado@alumnos.cucii.edu.mx', 2),
       ('Andrés', 'Guerrero', 'Luna', 'GULA020809HJCRNNG3', 'andres.guerrero@alumnos.cucii.edu.mx', 2),
       ('Paulina', 'Medina', 'Ochoa', 'MEOP010910MJCDCLQ9', 'paulina.medina@alumnos.cucii.edu.mx', 2),
       ('Alejandro', 'Flores', 'Martínez', 'FOMA960315HJCRRLA8', 'alejandro.flores@alumnos.cucii.edu.mx', 2),
       ('Daniela', 'Ríos', 'Castillo', 'RICD970822MJCSSNG8', 'daniela.rios@alumnos.cucii.edu.mx', 2),
       ('Miguel Ángel', 'Torres', 'Sánchez', 'TOSM951110HJCRNGN3', 'miguel.torres@alumnos.cucii.edu.mx', 2),
       ('Fernanda', 'Ortiz', 'García', 'OOGF980418MJCRRRO9', 'fernanda.ortiz@alumnos.cucii.edu.mx', 2),
       ('Jorge Alberto', 'Luna', 'Paredes', 'LUPJ940725HJCNRRI0', 'jorge.luna@alumnos.cucii.edu.mx', 2),
       ('Marcela', 'Reyes', 'Herrera', 'REHM990114MJCYRRY2', 'marcela.reyes@alumnos.cucii.edu.mx', 2),
       ('Raúl', 'Vargas', 'Jiménez', 'VAJR930930HJCRMLW6', 'raul.vargas@alumnos.cucii.edu.mx', 2),
       ('Tania', 'Navarro', 'Cruz', 'NACT000612MJCVZNK4', 'tania.navarro@alumnos.cucii.edu.mx', 2),
       ('Héctor', 'Peña', 'Contreras', 'PECH961205HJCNNCE3', 'hector.pena@alumnos.cucii.edu.mx', 2),
       ('Liliana', 'Campos', 'Vega', 'CAVL971028MJCMGLY5', 'liliana.campos@alumnos.cucii.edu.mx', 2),
       ('Óscar', 'Ramos', 'Ibarra', 'RAIO920420HJCMBSD1', 'oscar.ramos@alumnos.cucii.edu.mx', 2),
       ('Verónica', 'Salazar', 'Domínguez', 'SADV930715MJCLMRM1', 'veronica.salazar@alumnos.cucii.edu.mx', 2),
       ('Arturo', 'Soto', 'Aguilar', 'SOAA911108HJCTGRL5', 'arturo.soto@alumnos.cucii.edu.mx', 2),
       ('Mónica', 'Guerrero', 'Pacheco', 'GUPM940225MJCRCNT4', 'monica.guerrero@alumnos.cucii.edu.mx', 2),
       ('Gerardo', 'Esquivel', 'Medina', 'EEMG900812HJCSDRZ0', 'gerardo.esquivel@alumnos.cucii.edu.mx', 2),
       ('Patricia', 'Ávalos', 'Rentería', 'AARP950530MJCVNTX7', 'patricia.avalos@alumnos.cucii.edu.mx', 2),
       ('Enrique', 'Zúñiga', 'Galván', 'ZUGE921003HJCNLNR1', 'enrique.zuniga@alumnos.cucii.edu.mx', 2),
       ('Karina', 'Márquez', 'Fuentes', 'MAFK931218MJCRNRM1', 'karina.marquez@alumnos.cucii.edu.mx', 2),
       ('Saúl', 'Cárdenas', 'Osorio', 'CAOS910622HJCRSLR4', 'saul.cardenas@alumnos.cucii.edu.mx', 2),
       ('Dulce María', 'Gómez', 'Arredondo', 'GOAD950905MJCMRLU9', 'dulce.gomez@alumnos.cucii.edu.mx', 2),
       ('Ricardo', 'Vega', 'Mendoza', 'VEMR880310HJCGNCL9', 'ricardo.vega@alumnos.cucii.edu.mx', 2),
       ('Adriana', 'Cervantes', 'Lira', 'CELA900728MJCRRDG1', 'adriana.cervantes@alumnos.cucii.edu.mx', 2),
       ('Francisco Javier', 'Padilla', 'Rocha', 'PARF871115HJCDCNB3', 'francisco.padilla@alumnos.cucii.edu.mx', 2),
       ('Lorena', 'Sandoval', 'Camarena', 'SACL890422MJCNMRY4', 'lorena.sandoval@alumnos.cucii.edu.mx', 2),
       ('Eduardo', 'Trejo', 'Valenzuela', 'TEVE860805HJCJLDC3', 'eduardo.trejo@alumnos.cucii.edu.mx', 2),
       ('Claudia', 'Beltrán', 'Espinoza', 'BEEC910119MJCLSDD6', 'claudia.beltran@alumnos.cucii.edu.mx', 2),
       ('Sergio', 'Rangel', 'Cisneros', 'RACS851030HJCNSRI7', 'sergio.rangel@alumnos.cucii.edu.mx', 2),
       ('Alejandra', 'Figueroa', 'Larios', 'FILA920614MJCGRLU5', 'alejandra.figueroa@alumnos.cucii.edu.mx', 2),
       ('Roberto', 'Murillo', 'Zavala', 'MUZR871207HJCRVBF5', 'roberto.murillo@alumnos.cucii.edu.mx', 2),
       ('Mariana', 'Torre', 'Valdés', 'TOVM890525MJCRLRL3', 'mariana.torre@alumnos.cucii.edu.mx', 2);

-- ─── alumnos_grupos ───────────────────────────────────────────────────────────
insert into alumnos_grupos (alumno_id, grupo_id)
values (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1),
       (11, 2), (12, 2), (13, 2), (14, 2), (15, 2), (16, 2), (17, 2), (18, 2), (19, 2), (20, 2),
       (21, 3), (22, 3), (23, 3), (24, 3), (25, 3), (26, 3), (27, 3), (28, 3), (29, 3), (30, 3),
       (31, 4), (32, 4), (33, 4), (34, 4), (35, 4), (36, 4), (37, 4), (38, 4), (39, 4), (40, 4);

-- ─── profesores_grupos ────────────────────────────────────────────────────────
-- Ana Torres (3) → FP-101 en grupo 1 | Luis Pérez (4) → MD-101 en grupo 1
-- Elena Martínez (5) → MCI-101 en grupo 2 | Roberto Sánchez (6) → MCI-102 en grupo 2
-- Laura Castillo (7) → MAT-101 en grupo 3 | Jorge Flores (8) → MAT-102 en grupo 3
-- Patricia Ríos (9) → DCI-101 en grupo 4 | Fernando Díaz (10) → DCI-102 en grupo 4
insert into profesores_grupos (usuario_id, grupo_id, materia_id)
values (3, 1, 1), (4, 1, 2), (5, 2, 5), (6, 2, 6), (7, 3, 9), (8, 3, 10), (9, 4, 13), (10, 4, 14);

-- ─── calificaciones ───────────────────────────────────────────────────────────
insert into calificaciones (alumno_id, grupo_id, materia_id, calificacion, registrado_por)
values (1, 1, 1, 86.81, 3), (1, 1, 2, 88.07, 4),
       (2, 1, 1, 87.34, 3), (2, 1, 2, 62.86, 4),
       (3, 1, 1, 85.40, 3), (3, 1, 2, 81.37, 4),
       (4, 1, 1, 69.79, 3), (4, 1, 2, 78.49, 4),
       (5, 1, 1, 70.80, 3), (5, 1, 2, 97.02, 4),
       (6, 1, 1, 87.53, 3), (6, 1, 2, 68.78, 4),
       (7, 1, 1, 72.97, 3), (7, 1, 2, 90.73, 4),
       (8, 1, 1, 62.24, 3), (8, 1, 2, 92.87, 4),
       (9, 1, 1, 92.20, 3), (9, 1, 2, 76.05, 4),
       (10, 1, 1, 62.65, 3), (10, 1, 2, 96.53, 4),
       (11, 2, 5, 82.69, 5), (11, 2, 6, 88.72, 6),
       (12, 2, 5, 68.51, 5), (12, 2, 6, 79.97, 6),
       (13, 2, 5, 95.39, 5), (13, 2, 6, 85.71, 6),
       (14, 2, 5, 65.71, 5), (14, 2, 6, 65.59, 6),
       (15, 2, 5, 89.80, 5), (15, 2, 6, 81.56, 6),
       (16, 2, 5, 89.88, 5), (16, 2, 6, 77.14, 6),
       (17, 2, 5, 83.34, 5), (17, 2, 6, 74.48, 6),
       (18, 2, 5, 99.89, 5), (18, 2, 6, 65.53, 6),
       (19, 2, 5, 79.74, 5), (19, 2, 6, 90.23, 6),
       (20, 2, 5, 94.44, 5), (20, 2, 6, 66.11, 6),
       (21, 3, 9, 66.40, 7), (21, 3, 10, 87.22, 8),
       (22, 3, 9, 83.86, 7), (22, 3, 10, 75.39, 8),
       (23, 3, 9, 83.84, 7), (23, 3, 10, 78.72, 8),
       (24, 3, 9, 70.06, 7), (24, 3, 10, 82.13, 8),
       (25, 3, 9, 97.70, 7), (25, 3, 10, 87.21, 8),
       (26, 3, 9, 64.58, 7), (26, 3, 10, 95.39, 8),
       (27, 3, 9, 90.04, 7), (27, 3, 10, 90.74, 8),
       (28, 3, 9, 73.61, 7), (28, 3, 10, 71.74, 8),
       (29, 3, 9, 66.33, 7), (29, 3, 10, 60.13, 8),
       (30, 3, 9, 88.88, 7), (30, 3, 10, 88.79, 8),
       (31, 4, 13, 98.88, 9), (31, 4, 14, 90.48, 10),
       (32, 4, 13, 80.31, 9), (32, 4, 14, 64.26, 10),
       (33, 4, 13, 85.01, 9), (33, 4, 14, 93.67, 10),
       (34, 4, 13, 80.31, 9), (34, 4, 14, 67.96, 10),
       (35, 4, 13, 74.96, 9), (35, 4, 14, 66.46, 10),
       (36, 4, 13, 98.14, 9), (36, 4, 14, 96.90, 10),
       (37, 4, 13, 96.74, 9), (37, 4, 14, 83.96, 10),
       (38, 4, 13, 79.54, 9), (38, 4, 14, 64.47, 10),
       (39, 4, 13, 74.52, 9), (39, 4, 14, 99.41, 10),
       (40, 4, 13, 92.27, 9), (40, 4, 14, 69.58, 10);
