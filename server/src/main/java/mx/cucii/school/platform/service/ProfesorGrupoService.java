package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.ProfesorGrupoRequest;
import mx.cucii.school.platform.dto.ProfesorGrupoDetalleResponse;
import mx.cucii.school.platform.dto.ProfesorGrupoResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.ProfesorGrupo;
import mx.cucii.school.platform.model.ProfesorGrupoDetalle;
import mx.cucii.school.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfesorGrupoService {

    private final ProfesorGrupoJdbcRepository repository;
    private final UsuarioJdbcRepository usuarioRepository;
    private final GrupoJdbcRepository grupoRepository;
    private final MateriaJdbcRepository materiaRepository;

    public PageResponse<ProfesorGrupoDetalleResponse> findAll(int page, int size, Integer usuarioId,
                                                              Integer grupoId, Integer materiaId,
                                                              Boolean isActive, String search,
                                                              String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countDetalle(usuarioId, grupoId, materiaId, isActive, search);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<ProfesorGrupoDetalleResponse> content = repository.findDetalle(size, offset, usuarioId, grupoId, materiaId, isActive, search, sortBy, sortDir).stream()
                .map(this::toDetalleResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public ProfesorGrupoResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada: " + id));
    }

    @Transactional
    public ProfesorGrupoResponse create(ProfesorGrupoRequest request) {
        validateRequest(request);

        usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.usuarioId()));
        grupoRepository.findById(request.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + request.grupoId()));
        materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + request.materiaId()));

        repository.findByUsuarioIdAndGrupoIdAndMateriaId(
                request.usuarioId(), request.grupoId(), request.materiaId()
        ).ifPresent(a -> {
            throw new IllegalArgumentException("El profesor ya está asignado a este grupo y materia");
        });

        ProfesorGrupo entity = new ProfesorGrupo(
                null,
                request.usuarioId(),
                request.grupoId(),
                request.materiaId(),
                true,
                OffsetDateTime.now()
        );
        return toResponse(repository.save(entity));
    }

    @Transactional
    public ProfesorGrupoResponse update(Integer id, ProfesorGrupoRequest request) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada: " + id));
        validateRequest(request);

        usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.usuarioId()));
        grupoRepository.findById(request.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + request.grupoId()));
        materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + request.materiaId()));

        ProfesorGrupo updated = new ProfesorGrupo(
                id,
                request.usuarioId(),
                request.grupoId(),
                request.materiaId(),
                true,
                null
        );
        return toResponse(repository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Asignación no encontrada: " + id);
        }
        repository.softDeleteById(id);
    }

    private void validateRequest(ProfesorGrupoRequest request) {
        if (request.usuarioId() == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (request.grupoId() == null) {
            throw new IllegalArgumentException("El grupo es obligatorio");
        }
        if (request.materiaId() == null) {
            throw new IllegalArgumentException("La materia es obligatoria");
        }
    }

    private ProfesorGrupoResponse toResponse(ProfesorGrupo a) {
        return new ProfesorGrupoResponse(
                a.id(), a.usuarioId(), a.grupoId(), a.materiaId(),
                a.isActive(), a.createdAt()
        );
    }

    private ProfesorGrupoDetalleResponse toDetalleResponse(ProfesorGrupoDetalle d) {
        String nombre = ((d.usuarioNombre() == null ? "" : d.usuarioNombre()) + " "
                + (d.usuarioApellido() == null ? "" : d.usuarioApellido())).trim();
        return new ProfesorGrupoDetalleResponse(
                d.id(), d.usuarioId(), nombre,
                d.grupoId(), d.grupoClave(), d.grupoNombre(),
                d.materiaId(), d.materiaClave(), d.materiaNombre(),
                d.isActive(), d.createdAt()
        );
    }
}
