# CLAUDE.md — backend

La guía canónica y actualizada del backend es **[`AGENTS.md`](./AGENTS.md)**.

Resumen del estado actual:

- Spring Boot **4.0.6**, Java **21**, Maven wrapper, JdbcTemplate + SQL explícito.
- Spring Security 7 stateless con JWT (JJWT 0.12.6).
- PostgreSQL 18; esquema en `../database/db_structure.sql` y bootstrap en
  `../database/init/02_bootstrap.sh`.
- Perfil `prod` configurado por variables de entorno (`DB_*`, `JWT_*`,
  `CORS_ORIGINS`).
- Despliegue con `../server/Dockerfile` y `../docker-compose.prod.yaml`; guía en
  `../docs/DEPLOY.md`.
- Hitos pendientes en `../docs/HITOS_PENDIENTES.md`.

No dupliques aquí la arquitectura, endpoints ni convenciones: viven en
`AGENTS.md` para evitar documentación desactualizada.
