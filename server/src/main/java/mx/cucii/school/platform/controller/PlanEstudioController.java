package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasCountResponse;
import mx.cucii.school.platform.dto.PlanEstudioConMateriasResponse;
import mx.cucii.school.platform.dto.PlanEstudioRequest;
import mx.cucii.school.platform.dto.PlanEstudioResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.service.PlanEstudioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/planes-estudio")
@RequiredArgsConstructor
public class PlanEstudioController {

    private final PlanEstudioService planEstudioService;

    @GetMapping
    public ResponseEntity<PageResponse<PlanEstudioResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(planEstudioService.findAll(page, size, search, grado, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanEstudioResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(planEstudioService.findById(id));
    }

    @GetMapping("/{id}/con-materias")
    public ResponseEntity<PlanEstudioConMateriasResponse> getByIdWithMaterias(@PathVariable Integer id) {
        return ResponseEntity.ok(planEstudioService.findByIdWithMaterias(id));
    }

    @GetMapping("/con-materias-count")
    public ResponseEntity<PageResponse<PlanEstudioConMateriasCountResponse>> getAllWithMateriasCount(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(planEstudioService.findAllWithMateriasCount(page, size, search, grado, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}/con-materias-count")
    public ResponseEntity<PlanEstudioConMateriasCountResponse> getByIdWithMateriasCount(@PathVariable Integer id) {
        return ResponseEntity.ok(planEstudioService.findByIdWithMateriasCount(id));
    }

    @PostMapping
    public ResponseEntity<PlanEstudioResponse> create(@RequestBody PlanEstudioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planEstudioService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanEstudioResponse> update(@PathVariable Integer id,
                                                       @RequestBody PlanEstudioRequest request) {
        return ResponseEntity.ok(planEstudioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        planEstudioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<PlanEstudioResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(planEstudioService.restore(id));
    }
}
