package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.MateriaRequest;
import mx.cucii.school.platform.dto.MateriaResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.Materia;
import mx.cucii.school.platform.model.PlanEstudio;
import mx.cucii.school.platform.repository.MateriaJdbcRepository;
import mx.cucii.school.platform.repository.PlanEstudioJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaJdbcRepository materiaRepository;
    private final PlanEstudioJdbcRepository planEstudioRepository;

    public PageResponse<MateriaResponse> findAll(int page, int size, String search,
                                                 Integer planEstudioId, Integer cuatrimestre,
                                                 Boolean isActive, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = materiaRepository.countFiltered(search, planEstudioId, cuatrimestre, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<MateriaResponse> content = materiaRepository.findAll(size, offset, search, planEstudioId, cuatrimestre, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public MateriaResponse findById(Integer id) {
        return materiaRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }

    @Transactional
    public MateriaResponse create(MateriaRequest request) {
        PlanEstudio plan = planEstudioRepository.findById(request.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        validateCuatrimestre(request.cuatrimestre(), plan.duracionCuatrimestres());
        validateCreditos(request.creditos());
        OffsetDateTime now = OffsetDateTime.now();
        Materia nueva = new Materia(
                null,
                request.nombre(),
                request.clave(),
                request.creditos(),
                request.cuatrimestre(),
                request.planEstudioId(),
                true,
                now,
                now
        );
        return toResponse(materiaRepository.save(nueva));
    }

    @Transactional
    public MateriaResponse update(Integer id, MateriaRequest request) {
        Materia existing = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
        PlanEstudio plan = planEstudioRepository.findById(request.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado"));
        validateCuatrimestre(request.cuatrimestre(), plan.duracionCuatrimestres());
        validateCreditos(request.creditos());
        Materia updated = new Materia(
                existing.id(),
                request.nombre(),
                request.clave(),
                request.creditos(),
                request.cuatrimestre(),
                request.planEstudioId(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(materiaRepository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        Materia existing = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
        materiaRepository.softDeleteById(existing.id(), OffsetDateTime.now());
    }

    @Transactional
    public MateriaResponse restore(Integer id) {
        Materia existing = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
        if (!existing.isActive()) {
            materiaRepository.restoreById(id, OffsetDateTime.now());
        }
        return toResponse(materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id)));
    }

    private void validateCreditos(BigDecimal creditos) {
        if (creditos != null) {
            if (creditos.compareTo(BigDecimal.ZERO) < 0 ||
                creditos.compareTo(new BigDecimal("999.99")) > 0) {
                throw new IllegalArgumentException("Los créditos deben estar entre 0 y 999.99");
            }
        }
    }

    private void validateCuatrimestre(Integer cuatrimestre, Integer duracionCuatrimestres) {
        if (cuatrimestre != null && (cuatrimestre <= 0 || cuatrimestre > duracionCuatrimestres)) {
            throw new IllegalArgumentException(
                    "El cuatrimestre debe estar entre 1 y " + duracionCuatrimestres);
        }
    }

    private MateriaResponse toResponse(Materia m) {
        return new MateriaResponse(
                m.id(), m.nombre(), m.clave(), m.creditos(), m.cuatrimestre(),
                m.planEstudioId(), m.isActive(), m.createdAt(), m.updatedAt()
        );
    }
}
