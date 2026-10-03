package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.AlumnoGrupo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AlumnoGrupoJdbcRepository {

    private static final RowMapper<AlumnoGrupo> MAPPER = (rs, rowNum) ->
        new AlumnoGrupo(
                rs.getInt("id"),
                rs.getInt("alumno_id"),
                rs.getInt("grupo_id"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "alumno_id", "grupo_id", "is_active", "created_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public AlumnoGrupoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AlumnoGrupo> findAll() {
        return jdbcTemplate.query("SELECT * FROM alumnos_grupos", MAPPER);
    }

    public List<AlumnoGrupo> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos_grupos ORDER BY id ASC LIMIT ? OFFSET ?",
                MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alumnos_grupos", Long.class);
        return count != null ? count : 0;
    }

    public List<AlumnoGrupo> findAll(int limit, int offset, Integer alumnoId, Integer grupoId,
                                     Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM alumnos_grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (alumnoId != null) {
            sql.append(" AND alumno_id = ?");
            params.add(alumnoId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
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

    public long countFiltered(Integer alumnoId, Integer grupoId, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM alumnos_grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (alumnoId != null) {
            sql.append(" AND alumno_id = ?");
            params.add(alumnoId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<AlumnoGrupo> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos_grupos WHERE id = ?",
                MAPPER, id
        ).stream().findFirst();
    }

    public Optional<AlumnoGrupo> findByAlumnoIdAndGrupoId(Integer alumnoId, Integer grupoId) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos_grupos WHERE alumno_id = ? AND grupo_id = ?",
                MAPPER, alumnoId, grupoId
        ).stream().findFirst();
    }

    public List<AlumnoGrupo> findByAlumnoId(Integer alumnoId) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos_grupos WHERE alumno_id = ? AND is_active = true",
                MAPPER, alumnoId
        );
    }

    public List<AlumnoGrupo> findByGrupoId(Integer grupoId) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos_grupos WHERE grupo_id = ? AND is_active = true",
                MAPPER, grupoId
        );
    }

    public AlumnoGrupo save(AlumnoGrupo entity) {
        if (entity.id() == null) {
            return insert(entity);
        }
        return update(entity);
    }

    private AlumnoGrupo insert(AlumnoGrupo entity) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO alumnos_grupos (alumno_id, grupo_id, is_active, created_at)
                VALUES (?, ?, ?, ?)
                RETURNING *
                """,
                MAPPER,
                entity.alumnoId(), entity.grupoId(),
                entity.isActive(), entity.createdAt()
        );
    }

    private AlumnoGrupo update(AlumnoGrupo entity) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE alumnos_grupos
                SET alumno_id = ?, grupo_id = ?
                WHERE id = ?
                RETURNING *
                """,
                MAPPER,
                entity.alumnoId(), entity.grupoId(), entity.id()
        );
    }

    public void softDeleteById(Integer id) {
        jdbcTemplate.update(
                "UPDATE alumnos_grupos SET is_active = false WHERE id = ?", id
        );
    }

    public long countActiveByGrupo(Integer grupoId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alumnos_grupos WHERE grupo_id = ? AND is_active = true",
                Long.class, grupoId
        );
        return count != null ? count : 0;
    }

    public long countActiveByAlumno(Integer alumnoId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alumnos_grupos WHERE alumno_id = ? AND is_active = true",
                Long.class, alumnoId
        );
        return count != null ? count : 0;
    }
}
