package mx.cucii.school.platform.repository;

import mx.cucii.school.platform.model.Plantel;
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
public class PlantelJdbcRepository {

    private static final RowMapper<Plantel> PLANTEL_MAPPER = (rs, rowNum) ->
        new Plantel(
                rs.getInt("id"),
                rs.getString("nombre_oficial"),
                rs.getString("nombre_corto"),
                rs.getString("direccion_calle"),
                rs.getString("direccion_numero_ext"),
                rs.getString("direccion_numero_int"),
                rs.getString("colonia"),
                rs.getString("codigo_postal"),
                rs.getString("ciudad_municipio"),
                rs.getString("estado"),
                rs.getString("pais"),
                rs.getObject("latitud", BigDecimal.class),
                rs.getObject("longitud", BigDecimal.class),
                rs.getString("director_nombre"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "nombre_oficial", "nombre_corto", "ciudad_municipio", "estado",
            "pais", "is_active", "created_at", "updated_at"
    );

    private final JdbcTemplate jdbcTemplate;

    public PlantelJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Plantel> findAll() {
        return jdbcTemplate.query("SELECT * FROM planteles", PLANTEL_MAPPER);
    }

    public List<Plantel> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT * FROM planteles ORDER BY id ASC LIMIT ? OFFSET ?",
                PLANTEL_MAPPER, limit, offset
        );
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM planteles", Long.class);
        return count != null ? count : 0;
    }

    public List<Plantel> findAll(int limit, int offset, String search, String estado,
                                 String pais, Boolean isActive, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("SELECT * FROM planteles WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre_oficial ILIKE ? OR nombre_corto ILIKE ? OR ciudad_municipio ILIKE ? OR estado ILIKE ?)");
            String pattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) params.add(pattern);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append(" AND estado = ?");
            params.add(estado);
        }
        if (pais != null && !pais.isBlank()) {
            sql.append(" AND pais = ?");
            params.add(pais);
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

        return jdbcTemplate.query(sql.toString(), PLANTEL_MAPPER, params.toArray());
    }

    public long countFiltered(String search, String estado, String pais, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM planteles WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (nombre_oficial ILIKE ? OR nombre_corto ILIKE ? OR ciudad_municipio ILIKE ? OR estado ILIKE ?)");
            String pattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) params.add(pattern);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append(" AND estado = ?");
            params.add(estado);
        }
        if (pais != null && !pais.isBlank()) {
            sql.append(" AND pais = ?");
            params.add(pais);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public Optional<Plantel> findById(Integer id) {
        return jdbcTemplate.query(
                "SELECT * FROM planteles WHERE id = ?",
                PLANTEL_MAPPER, id
        ).stream().findFirst();
    }

    public Plantel save(Plantel plantel) {
        if (plantel.id() == null) {
            return insert(plantel);
        }
        return update(plantel);
    }

    private Plantel insert(Plantel plantel) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO planteles (nombre_oficial, nombre_corto, direccion_calle, direccion_numero_ext,
                                       direccion_numero_int, colonia, codigo_postal, ciudad_municipio,
                                       estado, pais, latitud, longitud, director_nombre,
                                       is_active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """,
                PLANTEL_MAPPER,
                plantel.nombreOficial(), plantel.nombreCorto(),
                plantel.direccionCalle(), plantel.direccionNumeroExt(),
                plantel.direccionNumeroInt(), plantel.colonia(),
                plantel.codigoPostal(), plantel.ciudadMunicipio(),
                plantel.estado(), plantel.pais(),
                plantel.latitud(), plantel.longitud(),
                plantel.directorNombre(),
                plantel.isActive(), plantel.createdAt(), plantel.updatedAt()
        );
    }

    private Plantel update(Plantel plantel) {
        return jdbcTemplate.queryForObject(
                """
                UPDATE planteles
                SET nombre_oficial = ?, nombre_corto = ?, direccion_calle = ?, direccion_numero_ext = ?,
                    direccion_numero_int = ?, colonia = ?, codigo_postal = ?, ciudad_municipio = ?,
                    estado = ?, pais = ?, latitud = ?, longitud = ?, director_nombre = ?,
                    updated_at = ?
                WHERE id = ?
                RETURNING *
                """,
                PLANTEL_MAPPER,
                plantel.nombreOficial(), plantel.nombreCorto(),
                plantel.direccionCalle(), plantel.direccionNumeroExt(),
                plantel.direccionNumeroInt(), plantel.colonia(),
                plantel.codigoPostal(), plantel.ciudadMunicipio(),
                plantel.estado(), plantel.pais(),
                plantel.latitud(), plantel.longitud(),
                plantel.directorNombre(),
                plantel.updatedAt(), plantel.id()
        );
    }

    public void softDeleteById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE planteles SET is_active = false, updated_at = ? WHERE id = ?",
                now, id
        );
    }

    public void restoreById(Integer id, OffsetDateTime now) {
        jdbcTemplate.update(
                "UPDATE planteles SET is_active = true, updated_at = ? WHERE id = ?",
                now, id
        );
    }
}
