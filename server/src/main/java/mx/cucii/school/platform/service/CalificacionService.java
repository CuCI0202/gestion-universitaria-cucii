package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.CalificacionRequest;
import mx.cucii.school.platform.dto.CalificacionResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.AlumnoGrupo;
import mx.cucii.school.platform.model.Calificacion;
import mx.cucii.school.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionJdbcRepository repository;
    private final AlumnoJdbcRepository alumnoRepository;
    private final GrupoJdbcRepository grupoRepository;
    private final MateriaJdbcRepository materiaRepository;
    private final UsuarioJdbcRepository usuarioRepository;
    private final AlumnoGrupoJdbcRepository alumnoGrupoRepository;

    public PageResponse<CalificacionResponse> findAll(int page, int size, Integer alumnoId,
                                                      Integer grupoId, Integer materiaId,
                                                      BigDecimal calificacionMin,
                                                      BigDecimal calificacionMax,
                                                      Integer registradoPor, Boolean isActive,
                                                      String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countFiltered(alumnoId, grupoId, materiaId, calificacionMin, calificacionMax, registradoPor, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<CalificacionResponse> content = repository.findAll(size, offset, alumnoId, grupoId, materiaId, calificacionMin, calificacionMax, registradoPor, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public CalificacionResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada: " + id));
    }

    @Transactional
    public CalificacionResponse create(CalificacionRequest request) {
        validateRequest(request);

        alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + request.alumnoId()));
        grupoRepository.findById(request.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + request.grupoId()));
        materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + request.materiaId()));
        if (request.registradoPor() != null) {
            usuarioRepository.findById(request.registradoPor())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.registradoPor()));
        }

        validateEnrollment(request.alumnoId(), request.grupoId());

        repository.findByAlumnoIdAndGrupoIdAndMateriaId(
                request.alumnoId(), request.grupoId(), request.materiaId()
        ).ifPresent(c -> {
            throw new IllegalArgumentException("El alumno ya tiene una calificación en esta materia y grupo");
        });

        OffsetDateTime now = OffsetDateTime.now();
        Calificacion entity = new Calificacion(
                null,
                request.alumnoId(),
                request.grupoId(),
                request.materiaId(),
                request.calificacion(),
                request.registradoPor(),
                true,
                now,
                now
        );
        return toResponse(repository.save(entity));
    }

    @Transactional
    public CalificacionResponse update(Integer id, CalificacionRequest request) {
        Calificacion existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada: " + id));
        validateRequest(request);

        alumnoRepository.findById(request.alumnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + request.alumnoId()));
        grupoRepository.findById(request.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + request.grupoId()));
        materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + request.materiaId()));
        if (request.registradoPor() != null) {
            usuarioRepository.findById(request.registradoPor())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.registradoPor()));
        }

        validateEnrollment(request.alumnoId(), request.grupoId());

        Calificacion updated = new Calificacion(
                existing.id(),
                request.alumnoId(),
                request.grupoId(),
                request.materiaId(),
                request.calificacion(),
                request.registradoPor(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(repository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Calificación no encontrada: " + id);
        }
        repository.softDeleteById(id, OffsetDateTime.now());
    }

    private void validateRequest(CalificacionRequest request) {
        if (request.alumnoId() == null) {
            throw new IllegalArgumentException("El alumno es obligatorio");
        }
        if (request.grupoId() == null) {
            throw new IllegalArgumentException("El grupo es obligatorio");
        }
        if (request.materiaId() == null) {
            throw new IllegalArgumentException("La materia es obligatoria");
        }
        if (request.calificacion() == null) {
            throw new IllegalArgumentException("La calificación es obligatoria");
        }
        BigDecimal calif = request.calificacion();
        if (calif.compareTo(BigDecimal.ZERO) < 0 || calif.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("La calificación debe estar entre 0 y 100");
        }
    }

    private void validateEnrollment(Integer alumnoId, Integer grupoId) {
        alumnoGrupoRepository.findByAlumnoIdAndGrupoId(alumnoId, grupoId)
                .filter(AlumnoGrupo::isActive)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El alumno no está asignado a este grupo"));
    }

    private CalificacionResponse toResponse(Calificacion c) {
        return new CalificacionResponse(
                c.id(), c.alumnoId(), c.grupoId(), c.materiaId(),
                c.calificacion(), c.registradoPor(),
                c.isActive(), c.createdAt(), c.updatedAt()
        );
    }
}
