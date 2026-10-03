package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Grupo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class GrupoJdbcRepository {

    private static final RowMapper<Grupo> GRUPO_MAPPER = (rs, rowNum) ->
        new Grupo(
                rs.getInt("id"),
                rs.getString("clave"),
                rs.getString("nombre"),
                rs.getInt("plan_estudio_id"),
                rs.getInt("plantel_id"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "clave", "nombre", "plan_estudio_id", "plantel_id",
            "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public GrupoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Grupo> findAll() {
        return jdbcTemplate.query("SELECT * FROM grupos", GRUPO_MAPPER);
    }

    public List<Grupo> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM grupos ORDER BY id ASC LIMIT ? OFFSET ?",
                GRUPO_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM grupos", Long.class);
        return count != null ? count : 0;
    }

    public List<Grupo> findAll(int limit, int offset, String search, Integer planEstudioId,
                               Integer plantelId, Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (clave ILIKE ? OR nombre ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (planEstudioId != null) {
            sql.append(" AND plan_estudio_id = ?");
            params.add(planEstudioId);
        }
        if (plantelId != null) {
            sql.append(" AND plantel_id = ?");
            params.add(plantelId);
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

        return jdbcTemplate.query(sql.toString(), GRUPO_MAPPER, params.toArray());
    }

    public long countFiltered(String search, Integer planEstudioId, Integer plantelId, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM grupos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (clave ILIKE ? OR nombre ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (planEstudioId != null) {
            sql.append(" AND plan_estudio_id = ?");
            params.add(planEstudioId);
        }
        if (plantelId != null) {
            sql.append(" AND plantel_id = ?");
            params.add(plantelId);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<Grupo> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM grupos WHERE id = ?",
                GRUPO_MAPPER, id
        ).stream().findFirst();
    }

    public Optional<Grupo> findByClave(String clave) {
        return jdbcTemplate.query(
                "SELECT * FROM grupos WHERE clave = ?",
                GRUPO_MAPPER, clave
        ).stream().findFirst();
    }

    public String generateNextClave() {
        Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('grupos_numero_seq')", Long.class);
        return "CG-" + nextVal;
    }

    public Grupo save(Grupo grupo) {
        if (grupo.id() == null) {
            return insert(grupo);
        }
        return update(grupo);
    }

    private Grupo insert(Grupo grupo) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO grupos (clave, nombre, plan_estudio_id, plantel_id, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                GRUPO_MAPPER,
                grupo.clave(), grupo.nombre(), grupo.planEstudioId(),
                grupo.plantelId(),
                grupo.isActive(), grupo.createdAt(), grupo.updatedAt()
        );
    }

    private Grupo update(Grupo grupo) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE grupos
                SET clave = ?, nombre = ?, plan_estudio_id = ?, plantel_id = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                GRUPO_MAPPER,
                grupo.clave(), grupo.nombre(), grupo.planEstudioId(),
                grupo.plantelId(),
                grupo.updatedAt(), grupo.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE grupos SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE grupos SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public long countActiveByPlantel(Integer plantelId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM grupos WHERE plantel_id = ? AND is_active = true",
                Long.class, plantelId
        );
        return count != null ? count : 0;
    }

    public long countActiveByPlanEstudio(Integer planEstudioId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM grupos WHERE plan_estudio_id = ? AND is_active = true",
                Long.class, planEstudioId
        );
        return count != null ? count : 0;
    }
}
