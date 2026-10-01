package mx.cucii.school.platform.model;

import java.time.OffsetDateTime;

public record ProfesorGrupoDetalle(
        Integer id,
        Integer usuarioId,
        String usuarioNombre,
        String usuarioApellido,
        Integer grupoId,
        String grupoClave,
        String grupoNombre,
        Integer materiaId,
        String materiaClave,
        String materiaNombre,
        boolean isActive,
        OffsetDateTime createdAt
) {}
