package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Alumno;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AlumnoJdbcRepository {

    private static final RowMapper<Alumno> ALUMNO_MAPPER = (rs, rowNum) ->
        new Alumno(
                rs.getInt("id"),
                rs.getString("nombres"),
                rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"),
                rs.getString("curp"),
                rs.getString("correo_institucional"),
                rs.getInt("estatus_id"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "nombres", "primer_apellido", "segundo_apellido", "curp",
            "correo_institucional", "estatus_id", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public AlumnoJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Alumno> findAll() {
        return jdbcTemplate.query("SELECT * FROM alumnos", ALUMNO_MAPPER);
    }

    public List<Alumno> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos ORDER BY id ASC LIMIT ? OFFSET ?",
                ALUMNO_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alumnos", Long.class);
        return count != null ? count : 0;
    }

    public List<Alumno> findAll(int limit, int offset, String search, String curp,
                                String correoInstitucional, Integer estatusId,
                                Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM alumnos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombres ILIKE ? OR primer_apellido ILIKE ? OR segundo_apellido ILIKE ? OR curp ILIKE ? OR correo_institucional ILIKE ?)");
            String pattern = "%" + search + "%";
            for (int i = 0; i < 5; i++) params.add(pattern);
        }
        if (curp != null && !curp.isBlank()) {
            sql.append(" AND curp = ?");
            params.add(curp);
        }
        if (correoInstitucional != null && !correoInstitucional.isBlank()) {
            sql.append(" AND correo_institucional = ?");
            params.add(correoInstitucional);
        }
        if (estatusId != null) {
            sql.append(" AND estatus_id = ?");
            params.add(estatusId);
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

        return jdbcTemplate.query(sql.toString(), ALUMNO_MAPPER, params.toArray());
    }

    public long countFiltered(String search, String curp, String correoInstitucional,
                              Integer estatusId, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM alumnos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombres ILIKE ? OR primer_apellido ILIKE ? OR segundo_apellido ILIKE ? OR curp ILIKE ? OR correo_institucional ILIKE ?)");
            String pattern = "%" + search + "%";
            for (int i = 0; i < 5; i++) params.add(pattern);
        }
        if (curp != null && !curp.isBlank()) {
            sql.append(" AND curp = ?");
            params.add(curp);
        }
        if (correoInstitucional != null && !correoInstitucional.isBlank()) {
            sql.append(" AND correo_institucional = ?");
            params.add(correoInstitucional);
        }
        if (estatusId != null) {
            sql.append(" AND estatus_id = ?");
            params.add(estatusId);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<Alumno> findByGrupo(Integer grupoId, int limit, int offset, String search, Boolean isActive) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.* FROM alumnos a JOIN alumnos_grupos ag ON ag.alumno_id = a.id "
                        + "WHERE ag.grupo_id = ? AND ag.is_active = true");
        List<Object> params = new ArrayList<>();
        params.add(grupoId);
        appendSearch(sql, params, "a", search);
        if (isActive != null) {
            sql.append(" AND a.is_active = ?");
            params.add(isActive);
        }
        sql.append(" ORDER BY a.primer_apellido ASC, a.segundo_apellido ASC, a.nombres ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), ALUMNO_MAPPER, params.toArray());
    }

    public long countByGrupo(Integer grupoId, String search, Boolean isActive) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM alumnos a JOIN alumnos_grupos ag ON ag.alumno_id = a.id "
                        + "WHERE ag.grupo_id = ? AND ag.is_active = true");
        List<Object> params = new ArrayList<>();
        params.add(grupoId);
        appendSearch(sql, params, "a", search);
        if (isActive != null) {
            sql.append(" AND a.is_active = ?");
            params.add(isActive);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<Alumno> findNotInGrupo(Integer grupoId, int limit, int offset, String search, Boolean isActive) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.* FROM alumnos a WHERE NOT EXISTS ("
                        + "SELECT 1 FROM alumnos_grupos ag WHERE ag.alumno_id = a.id AND ag.grupo_id = ? AND ag.is_active = true)");
        List<Object> params = new ArrayList<>();
        params.add(grupoId);
        appendSearch(sql, params, "a", search);
        if (isActive != null) {
            sql.append(" AND a.is_active = ?");
            params.add(isActive);
        }
        sql.append(" ORDER BY a.primer_apellido ASC, a.segundo_apellido ASC, a.nombres ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), ALUMNO_MAPPER, params.toArray());
    }

    public long countNotInGrupo(Integer grupoId, String search, Boolean isActive) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM alumnos a WHERE NOT EXISTS ("
                        + "SELECT 1 FROM alumnos_grupos ag WHERE ag.alumno_id = a.id AND ag.grupo_id = ? AND ag.is_active = true)");
        List<Object> params = new ArrayList<>();
        params.add(grupoId);
        appendSearch(sql, params, "a", search);
        if (isActive != null) {
            sql.append(" AND a.is_active = ?");
            params.add(isActive);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private void appendSearch(StringBuilder sql, List<Object> params, String alias, String search) {
        if (search == null || search.isBlank()) {
            return;
        }
        String c = alias == null ? "" : alias + ".";
        sql.append(" AND (").append(c).append("nombres ILIKE ? OR ")
                .append(c).append("primer_apellido ILIKE ? OR ")
                .append(c).append("segundo_apellido ILIKE ? OR ")
                .append(c).append("curp ILIKE ? OR ")
                .append(c).append("correo_institucional ILIKE ?)");
        String pattern = "%" + search + "%";
        for (int i = 0; i < 5; i++) {
            params.add(pattern);
        }
    }

    public Optional<Alumno> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos WHERE id = ?",
                ALUMNO_MAPPER, id
        ).stream().findFirst();
    }

    public Optional<Alumno> findByCurp(String curp) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos WHERE curp = ?",
                ALUMNO_MAPPER, curp
        ).stream().findFirst();
    }

    public Optional<Alumno> findByCorreoInstitucional(String correo) {
        return jdbcTemplate.query(
                "SELECT * FROM alumnos WHERE correo_institucional = ?",
                ALUMNO_MAPPER, correo
        ).stream().findFirst();
    }

    public Alumno save(Alumno alumno) {
        if (alumno.id() == null) {
            return insert(alumno);
        }
        return update(alumno);
    }

    private Alumno insert(Alumno alumno) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO alumnos (nombres, primer_apellido, segundo_apellido, curp, correo_institucional, estatus_id, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                ALUMNO_MAPPER,
                alumno.nombres(), alumno.primerApellido(), alumno.segundoApellido(),
                alumno.curp(), alumno.correoInstitucional(), alumno.estatusId(),
                alumno.isActive(), alumno.createdAt(), alumno.updatedAt()
        );
    }

    private Alumno update(Alumno alumno) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE alumnos
                SET nombres = ?, primer_apellido = ?, segundo_apellido = ?, curp = ?,
                    correo_institucional = ?, estatus_id = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                ALUMNO_MAPPER,
                alumno.nombres(), alumno.primerApellido(), alumno.segundoApellido(),
                alumno.curp(), alumno.correoInstitucional(), alumno.estatusId(),
                alumno.updatedAt(), alumno.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE alumnos SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE alumnos SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }
}
