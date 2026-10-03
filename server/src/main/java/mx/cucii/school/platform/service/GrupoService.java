package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.CantidadCuatrimestresResponse;
import mx.cucii.school.platform.dto.GrupoRequest;
import mx.cucii.school.platform.dto.GrupoResponse;
import mx.cucii.school.platform.dto.MateriaResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.Grupo;
import mx.cucii.school.platform.model.Materia;
import mx.cucii.school.platform.model.PlanEstudio;
import mx.cucii.school.platform.repository.GrupoJdbcRepository;
import mx.cucii.school.platform.repository.MateriaJdbcRepository;
import mx.cucii.school.platform.repository.PlanEstudioJdbcRepository;
import mx.cucii.school.platform.repository.PlantelJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoJdbcRepository repository;
    private final PlanEstudioJdbcRepository planEstudioRepository;
    private final PlantelJdbcRepository plantelRepository;
    private final MateriaJdbcRepository materiaRepository;

    public PageResponse<GrupoResponse> findAll(int page, int size, String search,
                                               Integer planEstudioId, Integer plantelId,
                                               Boolean isActive, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countFiltered(search, planEstudioId, plantelId, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<GrupoResponse> content = repository.findAll(size, offset, search, planEstudioId, plantelId, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public GrupoResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + id));
    }

    @Transactional
    public GrupoResponse create(GrupoRequest request) {
        validateRequest(request);

        planEstudioRepository.findById(request.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        plantelRepository.findById(request.plantelId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado"));

        String clave = request.clave();
        if (clave == null || clave.isBlank()) {
            clave = repository.generateNextClave();
        } else {
            String finalClave = clave;
            repository.findByClave(clave).ifPresent(g -> {
                throw new IllegalArgumentException("La clave ya está registrada: " + finalClave);
            });
        }

        OffsetDateTime now = OffsetDateTime.now();
        Grupo grupo = new Grupo(
                null,
                clave,
                request.nombre(),
                request.planEstudioId(),
                request.plantelId(),
                true,
                now,
                now
        );
        return toResponse(repository.save(grupo));
    }

    @Transactional
    public GrupoResponse update(Integer id, GrupoRequest request) {
        Grupo existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + id));
        validateRequest(request);

        planEstudioRepository.findById(request.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        plantelRepository.findById(request.plantelId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado"));

        String clave = request.clave();
        if (clave == null || clave.isBlank()) {
            clave = existing.clave();
        } else if (!clave.equals(existing.clave())) {
            String finalClave = clave;
            repository.findByClave(clave).ifPresent(g -> {
                throw new IllegalArgumentException("La clave ya está registrada: " + finalClave);
            });
        }

        Grupo updated = new Grupo(
                existing.id(),
                clave,
                request.nombre(),
                request.planEstudioId(),
                request.plantelId(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(repository.save(updated));
    }

    public CantidadCuatrimestresResponse getCuatrimestres(Integer grupoId) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + grupoId));
        PlanEstudio plan = planEstudioRepository.findById(grupo.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        return new CantidadCuatrimestresResponse(plan.duracionCuatrimestres());
    }

    public List<MateriaResponse> getMateriasByCuatrimestre(Integer grupoId, Integer cuatrimestre) {
        Grupo grupo = repository.findById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + grupoId));
        PlanEstudio plan = planEstudioRepository.findById(grupo.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        if (cuatrimestre <= 0 || cuatrimestre > plan.duracionCuatrimestres()) {
            throw new IllegalArgumentException(
                    "El cuatrimestre debe estar entre 1 y " + plan.duracionCuatrimestres());
        }
        return materiaRepository.findByPlanEstudioIdAndCuatrimestre(plan.id(), cuatrimestre).stream()
                .map(this::toMateriaResponse)
                .toList();
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Grupo no encontrado: " + id);
        }
        repository.softDeleteById(id, OffsetDateTime.now());
    }

    @Transactional
    public GrupoResponse restore(Integer id) {
        Grupo existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + id));
        if (!existing.isActive()) {
            repository.restoreById(id, OffsetDateTime.now());
        }
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + id)));
    }

    private void validateRequest(GrupoRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (request.planEstudioId() == null) {
            throw new IllegalArgumentException("El plan de estudio es obligatorio");
        }
        if (request.plantelId() == null) {
            throw new IllegalArgumentException("El plantel es obligatorio");
        }
    }

    private GrupoResponse toResponse(Grupo g) {
        return new GrupoResponse(
                g.id(), g.clave(), g.nombre(),
                g.planEstudioId(), g.plantelId(),
                g.isActive(), g.createdAt(), g.updatedAt()
        );
    }

    private MateriaResponse toMateriaResponse(Materia m) {
        return new MateriaResponse(
                m.id(), m.nombre(), m.clave(), m.creditos(), m.cuatrimestre(),
                m.planEstudioId(), m.isActive(), m.createdAt(), m.updatedAt()
        );
    }
}
