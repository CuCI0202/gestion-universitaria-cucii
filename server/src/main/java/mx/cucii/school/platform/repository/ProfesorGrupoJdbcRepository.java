package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.ProfesorGrupo;
import mx.cucii.school.platform.model.ProfesorGrupoDetalle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class ProfesorGrupoJdbcRepository {

    private static final RowMapper<ProfesorGrupo> MAPPER = (rs, rowNum) ->
        new ProfesorGrupo(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getInt("grupo_id"),
                rs.getInt("materia_id"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class)
        );

    private static final RowMapper<ProfesorGrupoDetalle> DETALLE_MAPPER = (rs, rowNum) ->
        new ProfesorGrupoDetalle(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getString("usuario_nombre"),
                rs.getString("usuario_apellido"),
                rs.getInt("grupo_id"),
                rs.getString("grupo_clave"),
                rs.getString("grupo_nombre"),
                rs.getInt("materia_id"),
                rs.getString("materia_clave"),
                rs.getString("materia_nombre"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class)
        );

    private static final String DETALLE_COLUMNS =
            "pg.id, pg.usuario_id, u.nombre AS usuario_nombre, u.apellido AS usuario_apellido, "
                    + "pg.grupo_id, g.clave AS grupo_clave, g.nombre AS grupo_nombre, "
                    + "pg.materia_id, m.clave AS materia_clave, m.nombre AS materia_nombre, "
                    + "pg.is_active, pg.created_at ";

    private static final String DETALLE_FROM =
            "FROM profesores_grupos pg "
                    + "LEFT JOIN usuarios u ON u.id = pg.usuario_id "
                    + "LEFT JOIN grupos g ON g.id = pg.grupo_id "
                    + "LEFT JOIN materias m ON m.id = pg.materia_id "
                    + "WHERE 1=1";

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "usuario_id", "grupo_id", "materia_id", "is_active", "created_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public ProfesorGrupoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProfesorGrupo> findAll() {
        return jdbcTemplate.query("SELECT * FROM profesores_grupos", MAPPER);
    }

    public List<ProfesorGrupo> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM profesores_grupos ORDER BY id ASC LIMIT ? OFFSET ?",
                MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM profesores_grupos", Long.class);
        return count != null ? count : 0;
    }

    public List<ProfesorGrupo> findAll(int limit, int offset, Integer usuarioId, Integer grupoId,
                                       Integer materiaId, Boolean isActive,
                                       String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM profesores_grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (usuarioId != null) {
            sql.append(" AND usuario_id = ?");
            params.add(usuarioId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
        }
        if (materiaId != null) {
            sql.append(" AND materia_id = ?");
            params.add(materiaId);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        String col = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "id";
        String dir = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append(" ORDER BY ").append(col).append(" ").append(dir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), MAPPER, params.toArray());
    }

    public long countFiltered(Integer usuarioId, Integer grupoId, Integer materiaId, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM profesores_grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (usuarioId != null) {
            sql.append(" AND usuario_id = ?");
            params.add(usuarioId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
        }
        if (materiaId != null) {
            sql.append(" AND materia_id = ?");
            params.add(materiaId);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<ProfesorGrupoDetalle> findDetalle(int limit, int offset, Integer usuarioId,
                                                  Integer grupoId, Integer materiaId, Boolean isActive,
                                                  String search, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT ").append(DETALLE_COLUMNS).append(DETALLE_FROM);
        List<Object> params = new ArrayList<>();
        appendDetailFilters(sql, params, usuarioId, grupoId, materiaId, isActive, search);

        String col = ALLOWED_SORT_COLUMNS.contains(sortBy) ? "pg." + sortBy : "pg.id";
        String dir = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append(" ORDER BY ").append(col).append(" ").append(dir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), DETALLE_MAPPER, params.toArray());
    }

    public long countDetalle(Integer usuarioId, Integer grupoId, Integer materiaId,
                             Boolean isActive, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) ").append(DETALLE_FROM);
        List<Object> params = new ArrayList<>();
        appendDetailFilters(sql, params, usuarioId, grupoId, materiaId, isActive, search);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private void appendDetailFilters(StringBuilder sql, List<Object> params, Integer usuarioId,
                                     Integer grupoId, Integer materiaId, Boolean isActive,
                                     String search) {
        if (usuarioId != null) {
            sql.append(" AND pg.usuario_id = ?");
            params.add(usuarioId);
        }
        if (grupoId != null) {
            sql.append(" AND pg.grupo_id = ?");
            params.add(grupoId);
        }
        if (materiaId != null) {
            sql.append(" AND pg.materia_id = ?");
            params.add(materiaId);
        }
        if (isActive != null) {
            sql.append(" AND pg.is_active = ?");
            params.add(isActive);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (u.nombre ILIKE ? OR u.apellido ILIKE ? "
                    + "OR (u.nombre || ' ' || u.apellido) ILIKE ? "
                    + "OR g.clave ILIKE ? OR g.nombre ILIKE ? "
                    + "OR m.clave ILIKE ? OR m.nombre ILIKE ?)");
            String pattern = "%" + search + "%";
            for (int i = 0; i < 7; i++) {
                params.add(pattern);
            }
        }
    }

    public Optional<ProfesorGrupo> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM profesores_grupos WHERE id = ?",
                MAPPER, id
        ).stream().findFirst();
    }

    public Optional<ProfesorGrupo> findByUsuarioIdAndGrupoIdAndMateriaId(
            Integer usuarioId, Integer grupoId, Integer materiaId) {
        return jdbcTemplate.query(
                "SELECT * FROM profesores_grupos WHERE usuario_id = ? AND grupo_id = ? AND materia_id = ?",
                MAPPER, usuarioId, grupoId, materiaId
        ).stream().findFirst();
    }

    public ProfesorGrupo save(ProfesorGrupo entity) {
        if (entity.id() == null) {
            return insert(entity);
        }
        return update(entity);
    }

    private ProfesorGrupo insert(ProfesorGrupo entity) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO profesores_grupos (usuario_id, grupo_id, materia_id, is_active, created_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING *
                """,
                MAPPER,
                entity.usuarioId(), entity.grupoId(), entity.materiaId(),
                entity.isActive(), entity.createdAt()
        );
    }

    private ProfesorGrupo update(ProfesorGrupo entity) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE profesores_grupos
                SET usuario_id = ?, grupo_id = ?, materia_id = ?
                WHERE id = ?
                RETURNING *
                """,
                MAPPER,
                entity.usuarioId(), entity.grupoId(), entity.materiaId(), entity.id()
        );
    }

    public void softDeleteById(Integer id) {
        jdbcTemplate.update(
                "UPDATE profesores_grupos SET is_active = false WHERE id = ?", id
        );
    }

    public long countActiveByGrupo(Integer grupoId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profesores_grupos WHERE grupo_id = ? AND is_active = true",
                Long.class, grupoId
        );
        return count != null ? count : 0;
    }

    public long countActiveByMateria(Integer materiaId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profesores_grupos WHERE materia_id = ? AND is_active = true",
                Long.class, materiaId
        );
        return count != null ? count : 0;
    }

    public long countActiveByUsuario(Integer usuarioId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profesores_grupos WHERE usuario_id = ? AND is_active = true",
                Long.class, usuarioId
        );
        return count != null ? count : 0;
    }
}
