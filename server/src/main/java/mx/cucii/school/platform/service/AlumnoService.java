package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.AlumnoRequest;
import mx.cucii.school.platform.dto.AlumnoResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.Alumno;
import mx.cucii.school.platform.repository.AlumnoGrupoJdbcRepository;
import mx.cucii.school.platform.repository.AlumnoJdbcRepository;
import mx.cucii.school.platform.repository.CalificacionJdbcRepository;
import mx.cucii.school.platform.repository.EstatusAlumnoJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlumnoService {

    private static final String CURP_REGEX = "^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|1[0-9]|2[0-9]|3[0-1])[HMX][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$";

    private final AlumnoJdbcRepository repository;
    private final EstatusAlumnoJdbcRepository estatusAlumnoRepository;
    private final AlumnoGrupoJdbcRepository alumnoGrupoRepository;
    private final CalificacionJdbcRepository calificacionRepository;

    public PageResponse<AlumnoResponse> findAll(int page, int size, String search, String curp,
                                                String correoInstitucional, Integer estatusId,
                                                Boolean isActive, String sortBy, String sortDir) {
        validatePaging(page, size);

        long totalElements = repository.countFiltered(search, curp, correoInstitucional, estatusId, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<AlumnoResponse> content = repository.findAll(size, offset, search, curp, correoInstitucional, estatusId, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public PageResponse<AlumnoResponse> findByGrupo(int page, int size, Integer grupoId, String search) {
        validatePaging(page, size);

        long totalElements = repository.countByGrupo(grupoId, search, null);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<AlumnoResponse> content = repository.findByGrupo(grupoId, size, offset, search, null).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public PageResponse<AlumnoResponse> findAvailableForGrupo(int page, int size, Integer grupoId, String search) {
        validatePaging(page, size);

        long totalElements = repository.countNotInGrupo(grupoId, search, true);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<AlumnoResponse> content = repository.findNotInGrupo(grupoId, size, offset, search, true).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    private void validatePaging(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");
    }

    public AlumnoResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
    }

    @Transactional
    public AlumnoResponse create(AlumnoRequest request) {
        validateRequest(request);

        repository.findByCurp(request.curp()).ifPresent(a -> {
            throw new IllegalArgumentException(a.isActive()
                    ? "El CURP ya está registrado"
                    : "Existe un alumno archivado con ese CURP. Restáuralo o usa otro.");
        });
        if (request.correoInstitucional() != null && !request.correoInstitucional().isBlank()) {
            repository.findByCorreoInstitucional(request.correoInstitucional()).ifPresent(a -> {
                throw new IllegalArgumentException(a.isActive()
                        ? "El correo institucional ya está registrado"
                        : "Existe un alumno archivado con ese correo. Restáuralo o usa otro.");
            });
        }

        OffsetDateTime now = OffsetDateTime.now();
        Alumno alumno = new Alumno(
                null,
                request.nombres(),
                request.primerApellido(),
                request.segundoApellido(),
                request.curp(),
                request.correoInstitucional(),
                request.estatusId(),
                true,
                now,
                now
        );
        return toResponse(repository.save(alumno));
    }

    @Transactional
    public AlumnoResponse update(Integer id, AlumnoRequest request) {
        Alumno existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
        validateRequest(request);

        if (!existing.curp().equals(request.curp())) {
            repository.findByCurp(request.curp()).ifPresent(a -> {
                throw new IllegalArgumentException(a.isActive()
                        ? "El CURP ya está registrado"
                        : "Existe un alumno archivado con ese CURP. Restáuralo o usa otro.");
            });
        }
        String newCorreo = request.correoInstitucional();
        if (newCorreo != null && !newCorreo.isBlank()
                && !newCorreo.equals(existing.correoInstitucional())) {
            repository.findByCorreoInstitucional(newCorreo).ifPresent(a -> {
                throw new IllegalArgumentException(a.isActive()
                        ? "El correo institucional ya está registrado"
                        : "Existe un alumno archivado con ese correo. Restáuralo o usa otro.");
            });
        }

        Alumno updated = new Alumno(
                existing.id(),
                request.nombres(),
                request.primerApellido(),
                request.segundoApellido(),
                request.curp(),
                request.correoInstitucional(),
                request.estatusId(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(repository.save(updated));
    }

    @Transactional
    public void delete(Integer id) {
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Alumno no encontrado: " + id);
        }
        long grupos = alumnoGrupoRepository.countActiveByAlumno(id);
        long calificaciones = calificacionRepository.countActiveByAlumno(id);
        if (grupos > 0 || calificaciones > 0) {
            List<String> dependencias = new ArrayList<>();
            if (grupos > 0) dependencias.add(grupos + " grupo(s) asignado(s)");
            if (calificaciones > 0) dependencias.add(calificaciones + " calificación(es) registrada(s)");
            throw new IllegalArgumentException(
                    "No se puede archivar el alumno: tiene " + String.join(" y ", dependencias));
        }
        repository.softDeleteById(id, OffsetDateTime.now());
    }

    @Transactional
    public AlumnoResponse restore(Integer id) {
        Alumno existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
        if (!existing.isActive()) {
            repository.restoreById(id, OffsetDateTime.now());
        }
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id)));
    }

    private void validateRequest(AlumnoRequest request) {
        if (request.nombres() == null || request.nombres().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (request.primerApellido() == null || request.primerApellido().isBlank()) {
            throw new IllegalArgumentException("El primer apellido es obligatorio");
        }
        if (request.curp() == null || request.curp().isBlank()) {
            throw new IllegalArgumentException("El CURP es obligatorio");
        }
        if (!request.curp().matches(CURP_REGEX)) {
            throw new IllegalArgumentException("El formato del CURP es inválido");
        }
        if (request.estatusId() == null) {
            throw new IllegalArgumentException("El estatus del alumno es obligatorio");
        }
        if (!estatusAlumnoRepository.existsById(request.estatusId())) {
            throw new IllegalArgumentException("El estatus del alumno no es válido");
        }
    }

    private AlumnoResponse toResponse(Alumno a) {
        return new AlumnoResponse(
                a.id(), a.nombres(), a.primerApellido(), a.segundoApellido(),
                a.curp(), a.correoInstitucional(), a.estatusId(),
                a.isActive(), a.createdAt(), a.updatedAt()
        );
    }
}
