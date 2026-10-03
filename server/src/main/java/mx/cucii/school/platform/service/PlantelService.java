package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.PlantelRequest;
import mx.cucii.school.platform.dto.PlantelResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.Plantel;
import mx.cucii.school.platform.repository.PlantelJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantelService {

    private static final String PAIS_DEFAULT = "México";

    private final PlantelJdbcRepository repository;

    public PageResponse<PlantelResponse> findAll(int page, int size, String search,
                                                 String estado, String pais, Boolean isActive,
                                                 String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = repository.countFiltered(search, estado, pais, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<PlantelResponse> content = repository.findAll(size, offset, search, estado, pais, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public PlantelResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado: " + id));
    }

    @Transactional
    public PlantelResponse create(PlantelRequest request) {
        validateRequest(request);
        OffsetDateTime now = OffsetDateTime.now();
        Plantel plantel = new Plantel(
                null,
                request.nombreOficial(),
                request.nombreCorto(),
                request.direccionCalle(),
                request.direccionNumeroExt(),
                request.direccionNumeroInt(),
                request.colonia(),
                request.codigoPostal(),
                request.ciudadMunicipio(),
                request.estado(),
                request.pais() != null ? request.pais() : PAIS_DEFAULT,
                request.latitud(),
                request.longitud(),
                request.directorNombre(),
                true,
                now,
                now
        );
        return toResponse(repository.save(plantel));
    }

    @Transactional
    public PlantelResponse update(Integer id, PlantelRequest request) {
        Plantel existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado: " + id));
        validateRequest(request);
        Plantel updated = new Plantel(
                existing.id(),
                request.nombreOficial(),
                request.nombreCorto(),
                request.direccionCalle(),
                request.direccionNumeroExt(),
                request.direccionNumeroInt(),
                request.colonia(),
                request.codigoPostal(),
                request.ciudadMunicipio(),
                request.estado(),
                request.pais() != null ? request.pais() : PAIS_DEFAULT,
                request.latitud(),
                request.longitud(),
                request.directorNombre(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(repository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Plantel no encontrado: " + id);
        }
        repository.softDeleteById(id, OffsetDateTime.now());
    }

    @Transactional
    public PlantelResponse restore(Integer id) {
        Plantel existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado: " + id));
        if (!existing.isActive()) {
            repository.restoreById(id, OffsetDateTime.now());
        }
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plantel no encontrado: " + id)));
    }

    private void validateRequest(PlantelRequest request) {
        if (request.nombreOficial() == null || request.nombreOficial().isBlank()) {
            throw new IllegalArgumentException("El nombre oficial es obligatorio");
        }
        if (request.ciudadMunicipio() == null || request.ciudadMunicipio().isBlank()) {
            throw new IllegalArgumentException("La ciudad o municipio es obligatorio");
        }
        if (request.estado() == null || request.estado().isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
    }

    private PlantelResponse toResponse(Plantel p) {
        return new PlantelResponse(
                p.id(), p.nombreOficial(), p.nombreCorto(),
                p.direccionCalle(), p.direccionNumeroExt(), p.direccionNumeroInt(),
                p.colonia(), p.codigoPostal(), p.ciudadMunicipio(),
                p.estado(), p.pais(), p.latitud(), p.longitud(),
                p.directorNombre(), p.isActive(), p.createdAt(), p.updatedAt()
        );
    }
}
