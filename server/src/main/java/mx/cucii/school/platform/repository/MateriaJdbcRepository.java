package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Materia;
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
public class MateriaJdbcRepository {

    private static final RowMapper<Materia> MATERIA_MAPPER = (rs, rowNum) ->
        new Materia(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("clave"),
                rs.getBigDecimal("creditos"),
                rs.getInt("cuatrimestre"),
                rs.getInt("plan_estudio_id"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "nombre", "clave", "creditos", "cuatrimestre",
            "plan_estudio_id", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public MateriaJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Materia> findAll() {
        return jdbcTemplate.query("SELECT * FROM materias", MATERIA_MAPPER);
    }

    public List<Materia> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM materias ORDER BY id ASC LIMIT ? OFFSET ?",
                MATERIA_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM materias", Long.class);
        return count != null ? count : 0;
    }

    public List<Materia> findAll(int limit, int offset, String search, Integer planEstudioId,
                                 Integer cuatrimestre, Boolean isActive,
                                 String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM materias WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR clave ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (planEstudioId != null) {
            sql.append(" AND plan_estudio_id = ?");
            params.add(planEstudioId);
        }
        if (cuatrimestre != null) {
            sql.append(" AND cuatrimestre = ?");
            params.add(cuatrimestre);
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

        return jdbcTemplate.query(sql.toString(), MATERIA_MAPPER, params.toArray());
    }

    public long countFiltered(String search, Integer planEstudioId, Integer cuatrimestre, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM materias WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR clave ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (planEstudioId != null) {
            sql.append(" AND plan_estudio_id = ?");
            params.add(planEstudioId);
        }
        if (cuatrimestre != null) {
            sql.append(" AND cuatrimestre = ?");
            params.add(cuatrimestre);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<Materia> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM materias WHERE id = ?",
                MATERIA_MAPPER, id
        ).stream().findFirst();
    }

    public List<Materia> findByPlanEstudioIdAndCuatrimestre(Integer planEstudioId, Integer cuatrimestre) {
        return jdbcTemplate.query(
                "SELECT * FROM materias WHERE plan_estudio_id = ? AND cuatrimestre = ?",
                MATERIA_MAPPER, planEstudioId, cuatrimestre
        );
    }

    public Materia save(Materia materia) {
        if (materia.id() == null) {
            return insert(materia);
        }
        return update(materia);
    }

    private Materia insert(Materia materia) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO materias (nombre, clave, creditos, cuatrimestre, plan_estudio_id, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                MATERIA_MAPPER,
                materia.nombre(), materia.clave(), materia.creditos(),
                materia.cuatrimestre(), materia.planEstudioId(),
                materia.isActive(), materia.createdAt(), materia.updatedAt()
        );
    }

    private Materia update(Materia materia) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE materias
                SET nombre = ?, clave = ?, creditos = ?, cuatrimestre = ?,
                    plan_estudio_id = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                MATERIA_MAPPER,
                materia.nombre(), materia.clave(), materia.creditos(),
                materia.cuatrimestre(), materia.planEstudioId(),
                materia.updatedAt(), materia.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE materias SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE materias SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }
}
