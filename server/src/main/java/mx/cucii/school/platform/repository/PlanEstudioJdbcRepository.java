package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.dto.MateriaResponse;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasCountResponse;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasResponse;
import mx.cucii.school.platform.model.Materia;
import mx.cucii.school.platform.model.PlanEstudio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class PlanEstudioJdbcRepository {

    private static final RowMapper<PlanEstudio> PLAN_MAPPER = (rs, rowNum) ->
        new PlanEstudio(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("grado"),
                rs.getString("numero_rvoe"),
                rs.getObject("fecha_rvoe", LocalDate.class),
                rs.getInt("duracion_cuatrimestres"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

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
            "id", "nombre", "grado", "numero_rvoe", "fecha_rvoe",
            "duracion_cuatrimestres", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public PlanEstudioJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlanEstudio> findAll() {
        return jdbcTemplate.query("SELECT * FROM planes_estudio", PLAN_MAPPER);
    }

    public List<PlanEstudio> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM planes_estudio ORDER BY id ASC LIMIT ? OFFSET ?",
                PLAN_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM planes_estudio", Long.class);
        return count != null ? count : 0;
    }

    public List<PlanEstudio> findAll(int limit, int offset, String search, String grado,
                                     Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM planes_estudio WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR numero_rvoe ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (grado != null && !grado.isBlank()) {
            sql.append(" AND grado = ?");
            params.add(grado);
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

        return jdbcTemplate.query(sql.toString(), PLAN_MAPPER, params.toArray());
    }

    public long countFiltered(String search, String grado, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM planes_estudio WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR numero_rvoe ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (grado != null && !grado.isBlank()) {
            sql.append(" AND grado = ?");
            params.add(grado);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<PlanEstudio> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM planes_estudio WHERE id = ?",
                PLAN_MAPPER, id
        ).stream().findFirst();
    }

    public boolean existsById(Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM planes_estudio WHERE id = ?",
                Integer.class, id
        );
        return count != null && count > 0;
    }

    public boolean existsByNumeroRvoe(String numeroRvoe) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM planes_estudio WHERE numero_rvoe = ?",
                Integer.class, numeroRvoe
        );
        return count != null && count > 0;
    }

    public boolean existsByNumeroRvoeAndIdNot(String numeroRvoe, Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM planes_estudio WHERE numero_rvoe = ? AND id != ?",
                Integer.class, numeroRvoe, id
        );
        return count != null && count > 0;
    }

    public PlanEstudio save(PlanEstudio plan) {
        if (plan.id() == null) {
            return insert(plan);
        }
        return update(plan);
    }

    private PlanEstudio insert(PlanEstudio plan) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO planes_estudio (nombre, grado, numero_rvoe, fecha_rvoe, duracion_cuatrimestres, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                PLAN_MAPPER,
                plan.nombre(), plan.grado(), plan.numeroRvoe(),
                plan.fechaRvoe(), plan.duracionCuatrimestres(),
                plan.isActive(), plan.createdAt(), plan.updatedAt()
        );
    }

    private PlanEstudio update(PlanEstudio plan) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE planes_estudio
                SET nombre = ?, grado = ?, numero_rvoe = ?, fecha_rvoe = ?,
                    duracion_cuatrimestres = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                PLAN_MAPPER,
                plan.nombre(), plan.grado(), plan.numeroRvoe(),
                plan.fechaRvoe(), plan.duracionCuatrimestres(),
                plan.updatedAt(), plan.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE planes_estudio SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE planes_estudio SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void softDeleteMateriasByPlanEstudioId(Integer planEstudioId, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE materias SET is_active = false, updated_at = ? WHERE plan_estudio_id = ? AND is_active = true",
                now, planEstudioId
        );
    }

    public void restoreMateriasByPlanEstudioId(Integer planEstudioId, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE materias SET is_active = true, updated_at = ? WHERE plan_estudio_id = ? AND is_active = false",
                now, planEstudioId
        );
    }

    public List<Materia> findMateriasByPlanEstudioId(Integer planEstudioId) {
        return jdbcTemplate.query(
                "SELECT * FROM materias WHERE plan_estudio_id = ? AND is_active = true",
                MATERIA_MAPPER, planEstudioId
        );
    }

    public PlanEstudioConMateriasResponse findByIdWithMaterias(Integer id) {
        String sql = """
            SELECT pe.id           AS plan_id,
                   pe.nombre       AS plan_nombre,
                   pe.grado,
                   pe.numero_rvoe,
                   pe.fecha_rvoe,
                   pe.duracion_cuatrimestres,
                   pe.is_active    AS plan_active,
                   pe.created_at   AS plan_created,
                   pe.updated_at   AS plan_updated,
                   m.id            AS materia_id,
                   m.nombre        AS materia_nombre,
                   m.clave,
                   m.creditos,
                   m.cuatrimestre,
                   m.plan_estudio_id,
                   m.is_active     AS materia_active,
                   m.created_at    AS materia_created,
                   m.updated_at    AS materia_updated
            FROM planes_estudio pe
            LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
            WHERE pe.id = ?
            """;

        return jdbcTemplate.query(sql, extractPlanWithMaterias(id), id);
    }

    public PlanEstudioConMateriasCountResponse findByIdWithMateriasCount(Integer id) {
        String sql = """
            SELECT pe.*,
                   COUNT(m.id) AS cantidad_materias
            FROM planes_estudio pe
            LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
            WHERE pe.id = ?
            GROUP BY pe.id
            """;

        return jdbcTemplate.query(sql, PLAN_CON_MATERIAS_COUNT_MAPPER, id)
                .stream().findFirst().orElse(null);
    }

    public List<PlanEstudioConMateriasCountResponse> findAllWithMateriasCount() {
        String sql = """
            SELECT pe.*,
                   COUNT(m.id) AS cantidad_materias
            FROM planes_estudio pe
            LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
            GROUP BY pe.id
            """;

        return jdbcTemplate.query(sql, PLAN_CON_MATERIAS_COUNT_MAPPER);
    }

    public List<PlanEstudioConMateriasCountResponse> findAllWithMateriasCount(int limit, int offset) {
        String sql = """
            SELECT pe.*,
                   COUNT(m.id) AS cantidad_materias
            FROM planes_estudio pe
            LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
            GROUP BY pe.id
            ORDER BY pe.id ASC
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, PLAN_CON_MATERIAS_COUNT_MAPPER, limit, offset);
    }

    public List<PlanEstudioConMateriasCountResponse> findAllWithMateriasCount(int limit, int offset,
            String search, String grado, Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("""
            SELECT pe.*,
                   COUNT(m.id) AS cantidad_materias
            FROM planes_estudio pe
            LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (pe.nombre ILIKE ? OR pe.numero_rvoe ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (grado != null && !grado.isBlank()) {
            sql.append(" AND pe.grado = ?");
            params.add(grado);
        }
        if (isActive != null) {
            sql.append(" AND pe.is_active = ?");
            params.add(isActive);
        }

        String col = ALLOWED_SORT_COLUMNS.contains(sortBy) ? "pe." + sortBy : "pe.id";
        String dir = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append(" GROUP BY pe.id");
        sql.append(" ORDER BY ").append(col).append(" ").append(dir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), PLAN_CON_MATERIAS_COUNT_MAPPER, params.toArray());
    }

    public long countAllWithMateriasCount() {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM planes_estudio pe
                LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
                GROUP BY pe.id
            ) sub
            """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    public long countAllWithMateriasCountFiltered(String search, String grado, Boolean isActive) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM planes_estudio pe
                LEFT JOIN materias m ON m.plan_estudio_id = pe.id AND m.is_active = true
                WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (pe.nombre ILIKE ? OR pe.numero_rvoe ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (grado != null && !grado.isBlank()) {
            sql.append(" AND pe.grado = ?");
            params.add(grado);
        }
        if (isActive != null) {
            sql.append(" AND pe.is_active = ?");
            params.add(isActive);
        }

        sql.append(" GROUP BY pe.id) sub");

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private static final RowMapper<PlanEstudioConMateriasCountResponse> PLAN_CON_MATERIAS_COUNT_MAPPER = (rs, rowNum) ->
        new PlanEstudioConMateriasCountResponse(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("grado"),
                rs.getString("numero_rvoe"),
                rs.getObject("fecha_rvoe", LocalDate.class),
                rs.getInt("duracion_cuatrimestres"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getInt("cantidad_materias")
        );

    private ResultSetExtractor<PlanEstudioConMateriasResponse> extractPlanWithMaterias(Integer id) {
        return (ResultSet rs) -> {
            PlanEstudioConMateriasResponse response = null;
            List<MateriaResponse> materias = new ArrayList<>();

            while (rs.next()) {
                if (response == null) {
                    response = new PlanEstudioConMateriasResponse(
                            rs.getInt("plan_id"),
                            rs.getString("plan_nombre"),
                            rs.getString("grado"),
                            rs.getString("numero_rvoe"),
                            rs.getObject("fecha_rvoe", LocalDate.class),
                            rs.getInt("duracion_cuatrimestres"),
                            rs.getBoolean("plan_active"),
                            rs.getObject("plan_created", OffsetDateTime.class),
                            rs.getObject("plan_updated", OffsetDateTime.class),
                            List.of()
                    );
                }
                int materiaId = rs.getInt("materia_id");
                if (!rs.wasNull()) {
                    materias.add(new MateriaResponse(
                            materiaId,
                            rs.getString("materia_nombre"),
                            rs.getString("clave"),
                            rs.getBigDecimal("creditos"),
                            rs.getInt("cuatrimestre"),
                            rs.getInt("plan_estudio_id"),
                            rs.getBoolean("materia_active"),
                            rs.getObject("materia_created", OffsetDateTime.class),
                            rs.getObject("materia_updated", OffsetDateTime.class)
                    ));
                }
            }

            if (response == null) {
                return null;
            }

            return new PlanEstudioConMateriasResponse(
                    response.id(), response.nombre(), response.grado(), response.numeroRvoe(),
                    response.fechaRvoe(), response.duracionCuatrimestres(),
                    response.isActive(), response.createdAt(), response.updatedAt(),
                    materias
            );
        };
    }
}
