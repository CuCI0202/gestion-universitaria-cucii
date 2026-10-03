package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasCountResponse;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasResponse;
import mx.cucii.school.platform.dto.PlanEstudioRequest;
import mx.cucii.school.platform.dto.PlanEstudioResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.PlanEstudio;
import mx.cucii.school.platform.repository.PlanEstudioJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlanEstudioService {

    private static final Set<String> GRADOS_VALIDOS = Set.of("Licenciatura", "Maestría", "Doctorado");

    private final PlanEstudioJdbcRepository repository;

    public PageResponse<PlanEstudioResponse> findAll(int page, int size, String search,
                                                     String grado, Boolean isActive,
                                                     String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countFiltered(search, grado, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<PlanEstudioResponse> content = repository.findAll(size, offset, search, grado, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public PlanEstudioResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + id));
    }

    public PlanEstudioConMateriasResponse findByIdWithMaterias(Integer id) {
        PlanEstudioConMateriasResponse result = repository.findByIdWithMaterias(id);
        if (result == null) {
            throw new ResourceNotFoundException("Plan de estudio no encontrado: " + id);
        }
        return result;
    }

    public PageResponse<PlanEstudioConMateriasCountResponse> findAllWithMateriasCount(int page, int size,
            String search, String grado, Boolean isActive, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countAllWithMateriasCountFiltered(search, grado, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<PlanEstudioConMateriasCountResponse> content = repository.findAllWithMateriasCount(size, offset, search, grado, isActive, sortBy, sortDir);

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public PlanEstudioConMateriasCountResponse findByIdWithMateriasCount(Integer id) {
        PlanEstudioConMateriasCountResponse result = repository.findByIdWithMateriasCount(id);
        if (result == null) {
            throw new ResourceNotFoundException("Plan de estudio no encontrado: " + id);
        }
        return result;
    }

    @Transactional
    public PlanEstudioResponse create(PlanEstudioRequest request) {
        validateRequest(request);

        if (repository.existsByNumeroRvoe(request.numeroRvoe())) {
            throw new IllegalArgumentException("El RVOE ya está registrado");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PlanEstudio nuevo = new PlanEstudio(
                null,
                request.nombre(),
                request.grado(),
                request.numeroRvoe(),
                request.fechaRvoe(),
                request.duracionCuatrimestres(),
                true,
                now,
                now
        );
        return toResponse(repository.save(nuevo));
    }

    @Transactional
    public PlanEstudioResponse update(Integer id, PlanEstudioRequest request) {
        validateRequest(request);

        PlanEstudio existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + id));

        if (!existing.numeroRvoe().equals(request.numeroRvoe())
                && repository.existsByNumeroRvoe(request.numeroRvoe())) {
            throw new IllegalArgumentException("El RVOE ya está registrado");
        }

        PlanEstudio updated = new PlanEstudio(
                existing.id(),
                request.nombre(),
                request.grado(),
                request.numeroRvoe(),
                request.fechaRvoe(),
                request.duracionCuatrimestres(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(repository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Plan de estudio no encontrado: " + id);
        }

        OffsetDateTime now = OffsetDateTime.now();
        repository.softDeleteMateriasByPlanEstudioId(id, now);
        repository.softDeleteById(id, now);
    }

    @Transactional
    public PlanEstudioResponse restore(Integer id) {
        PlanEstudio existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + id));
        if (!existing.isActive()) {
            OffsetDateTime now = OffsetDateTime.now();
            repository.restoreMateriasByPlanEstudioId(id, now);
            repository.restoreById(id, now);
        }
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + id)));
    }

    private void validateRequest(PlanEstudioRequest request) {
        if (!GRADOS_VALIDOS.contains(request.grado())) {
            throw new IllegalArgumentException("Grado inválido. Valores permitidos: Licenciatura, Maestría, Doctorado");
        }
        if (request.duracionCuatrimestres() == null || request.duracionCuatrimestres() <= 0) {
            throw new IllegalArgumentException("La duración en cuatrimestres debe ser mayor a 0");
        }
    }

    private PlanEstudioResponse toResponse(PlanEstudio p) {
        return new PlanEstudioResponse(
                p.id(), p.nombre(), p.grado(), p.numeroRvoe(),
                p.fechaRvoe(), p.duracionCuatrimestres(),
                p.isActive(), p.createdAt(), p.updatedAt()
        );
    }
}
