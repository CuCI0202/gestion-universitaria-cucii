package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UsuarioJdbcRepository {

    private static final RowMapper<Usuario> USUARIO_MAPPER = (rs, rowNum) ->
        new Usuario(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getInt("rol_id"),
                rs.getObject("plantel_id", Integer.class),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "nombre", "apellido", "email", "rol_id",
            "plantel_id", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public UsuarioJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Usuario> findAll() {
        return jdbcTemplate.query("SELECT * FROM usuarios", USUARIO_MAPPER);
    }

    public List<Usuario> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM usuarios ORDER BY id ASC LIMIT ? OFFSET ?",
                USUARIO_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        return count != null ? count : 0;
    }

    public List<Usuario> findAll(int limit, int offset, String search, Integer rolId,
                                 Integer plantelId, Boolean isActive,
                                 String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM usuarios WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR apellido ILIKE ? OR email ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (rolId != null) {
            sql.append(" AND rol_id = ?");
            params.add(rolId);
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

        return jdbcTemplate.query(sql.toString(), USUARIO_MAPPER, params.toArray());
    }

    public long countFiltered(String search, Integer rolId, Integer plantelId, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM usuarios WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre ILIKE ? OR apellido ILIKE ? OR email ILIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (rolId != null) {
            sql.append(" AND rol_id = ?");
            params.add(rolId);
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

    public Optional<Usuario> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM usuarios WHERE id = ?",
                USUARIO_MAPPER, id
        ).stream().findFirst();
    }

    public Optional<Usuario> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT * FROM usuarios WHERE email = ?",
                USUARIO_MAPPER, email
        ).stream().findFirst();
    }

    public Usuario save(Usuario usuario) {
        if (usuario.id() == null) {
            return insert(usuario);
        }
        return update(usuario);
    }

    private Usuario insert(Usuario usuario) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO usuarios (nombre, apellido, email, password_hash, rol_id, plantel_id, is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                USUARIO_MAPPER,
                usuario.nombre(), usuario.apellido(), usuario.email(), usuario.passwordHash(),
                usuario.rolId(), usuario.plantelId(),
                usuario.isActive(), usuario.createdAt(), usuario.updatedAt()
        );
    }

    private Usuario update(Usuario usuario) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE usuarios
                SET nombre = ?, apellido = ?, email = ?, password_hash = ?, rol_id = ?,
                    plantel_id = ?, is_active = ?, updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                USUARIO_MAPPER,
                usuario.nombre(), usuario.apellido(), usuario.email(), usuario.passwordHash(),
                usuario.rolId(), usuario.plantelId(),
                usuario.isActive(), usuario.updatedAt(), usuario.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE usuarios SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE usuarios SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void deleteById(Integer id) {
        jdbcTemplate.update("DELETE FROM usuarios WHERE id = ?", id);
    }

    public long countActiveByPlantel(Integer plantelId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE plantel_id = ? AND is_active = true",
                Long.class, plantelId
        );
        return count != null ? count : 0;
    }
}
