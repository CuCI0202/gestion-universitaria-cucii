package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.AlumnoResponse;
import mx.cucii.school.platform.dto.CantidadCuatrimestresResponse;
import mx.cucii.school.platform.dto.GrupoRequest;
import mx.cucii.school.platform.dto.GrupoResponse;
import mx.cucii.school.platform.dto.MateriaResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.service.AlumnoService;
import mx.cucii.school.platform.service.GrupoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupoService;
    private final AlumnoService alumnoService;

    @GetMapping
    public ResponseEntity<PageResponse<GrupoResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer planEstudioId,
            @RequestParam(required = false) Integer plantelId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(grupoService.findAll(page, size, search, planEstudioId, plantelId, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(grupoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<GrupoResponse> create(@RequestBody GrupoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grupoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoResponse> update(@PathVariable Integer id,
                                                 @RequestBody GrupoRequest request) {
        return ResponseEntity.ok(grupoService.update(id, request));
    }

    @GetMapping("/{id}/cuatrimestres")
    public ResponseEntity<CantidadCuatrimestresResponse> getCuatrimestres(@PathVariable Integer id) {
        return ResponseEntity.ok(grupoService.getCuatrimestres(id));
    }

    @GetMapping("/{id}/alumnos")
    public ResponseEntity<PageResponse<AlumnoResponse>> getAlumnos(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(alumnoService.findByGrupo(page, size, id, search));
    }

    @GetMapping("/{id}/alumnos-disponibles")
    public ResponseEntity<PageResponse<AlumnoResponse>> getAlumnosDisponibles(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(alumnoService.findAvailableForGrupo(page, size, id, search));
    }

    @GetMapping("/{id}/cuatrimestres/{cuatrimestre}/materias")
    public ResponseEntity<List<MateriaResponse>> getMateriasByCuatrimestre(
            @PathVariable Integer id,
            @PathVariable Integer cuatrimestre) {
        return ResponseEntity.ok(grupoService.getMateriasByCuatrimestre(id, cuatrimestre));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        grupoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<GrupoResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(grupoService.restore(id));
    }
}
