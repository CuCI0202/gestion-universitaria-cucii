package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Calificacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class CalificacionJdbcRepository {

    private static final RowMapper<Calificacion> MAPPER = (rs, rowNum) ->
        new Calificacion(
                rs.getInt("id"),
                rs.getInt("alumno_id"),
                rs.getInt("grupo_id"),
                rs.getInt("materia_id"),
                rs.getBigDecimal("calificacion"),
                rs.getObject("registrado_por", Integer.class),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "alumno_id", "grupo_id", "materia_id", "calificacion",
            "registrado_por", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public CalificacionJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Calificacion> findAll() {
        return jdbcTemplate.query("SELECT * FROM calificaciones", MAPPER);
    }

    public List<Calificacion> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM calificaciones ORDER BY id ASC LIMIT ? OFFSET ?",
                MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM calificaciones", Long.class);
        return count != null ? count : 0;
    }

    public List<Calificacion> findAll(int limit, int offset, Integer alumnoId, Integer grupoId,
                                      Integer materiaId, BigDecimal calificacionMin,
                                      BigDecimal calificacionMax, Integer registradoPor,
                                      Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM calificaciones WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (alumnoId != null) {
            sql.append(" AND alumno_id = ?");
            params.add(alumnoId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
        }
        if (materiaId != null) {
            sql.append(" AND materia_id = ?");
            params.add(materiaId);
        }
        if (calificacionMin != null) {
            sql.append(" AND calificacion >= ?");
            params.add(calificacionMin);
        }
        if (calificacionMax != null) {
            sql.append(" AND calificacion <= ?");
            params.add(calificacionMax);
        }
        if (registradoPor != null) {
            sql.append(" AND registrado_por = ?");
            params.add(registradoPor);
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

    public long countFiltered(Integer alumnoId, Integer grupoId, Integer materiaId,
                              BigDecimal calificacionMin, BigDecimal calificacionMax,
                              Integer registradoPor, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM calificaciones WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (alumnoId != null) {
            sql.append(" AND alumno_id = ?");
            params.add(alumnoId);
        }
        if (grupoId != null) {
            sql.append(" AND grupo_id = ?");
            params.add(grupoId);
        }
        if (materiaId != null) {
            sql.append(" AND materia_id = ?");
            params.add(materiaId);
        }
        if (calificacionMin != null) {
            sql.append(" AND calificacion >= ?");
            params.add(calificacionMin);
        }
        if (calificacionMax != null) {
            sql.append(" AND calificacion <= ?");
            params.add(calificacionMax);
        }
        if (registradoPor != null) {
            sql.append(" AND registrado_por = ?");
            params.add(registradoPor);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<Calificacion> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM calificaciones WHERE id = ?",
                MAPPER, id
        ).stream().findFirst();
    }

    public Optional<Calificacion> findByAlumnoIdAndGrupoIdAndMateriaId(
            Integer alumnoId, Integer grupoId, Integer materiaId) {
        return jdbcTemplate.query(
                "SELECT * FROM calificaciones WHERE alumno_id = ? AND grupo_id = ? AND materia_id = ?",
                MAPPER, alumnoId, grupoId, materiaId
        ).stream().findFirst();
    }

    public Calificacion save(Calificacion entity) {
        if (entity.id() == null) {
            return insert(entity);
        }
        return update(entity);
    }

    private Calificacion insert(Calificacion entity) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO calificaciones (alumno_id, grupo_id, materia_id, calificacion, registrado_por, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                MAPPER,
                entity.alumnoId(), entity.grupoId(), entity.materiaId(),
                entity.calificacion(), entity.registradoPor(),
                entity.isActive(), entity.createdAt(), entity.updatedAt()
        );
    }

    private Calificacion update(Calificacion entity) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE calificaciones
                SET alumno_id = ?, grupo_id = ?, materia_id = ?, calificacion = ?,
                    registrado_por = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                MAPPER,
                entity.alumnoId(), entity.grupoId(), entity.materiaId(),
                entity.calificacion(), entity.registradoPor(),
                entity.updatedAt(), entity.id()
        );
    }

    public List<Calificacion> findByAlumnoId(Integer alumnoId) {
        return jdbcTemplate.query(
                "SELECT * FROM calificaciones WHERE alumno_id = ?",
                MAPPER, alumnoId
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE calificaciones SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public long countActiveByGrupo(Integer grupoId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM calificaciones WHERE grupo_id = ? AND is_active = true",
                Long.class, grupoId
        );
        return count != null ? count : 0;
    }

    public long countActiveByMateria(Integer materiaId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM calificaciones WHERE materia_id = ? AND is_active = true",
                Long.class, materiaId
        );
        return count != null ? count : 0;
    }

    public long countActiveByAlumno(Integer alumnoId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM calificaciones WHERE alumno_id = ? AND is_active = true",
                Long.class, alumnoId
        );
        return count != null ? count : 0;
    }
}
