package mx.cucii.school.platform.dto;

import java.time.OffsetDateTime;

public record ProfesorGrupoDetalleResponse(
        Integer id,
        Integer usuarioId,
        String usuarioNombre,
        Integer grupoId,
        String grupoClave,
        String grupoNombre,
        Integer materiaId,
        String materiaClave,
        String materiaNombre,
        boolean isActive,
        OffsetDateTime createdAt
) {}
