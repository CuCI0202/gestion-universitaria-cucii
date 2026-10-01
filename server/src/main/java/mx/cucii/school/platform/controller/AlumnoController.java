package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.AlumnoRequest;
import mx.cucii.school.platform.dto.AlumnoResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.service.AlumnoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;

    @GetMapping
    public ResponseEntity<PageResponse<AlumnoResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String correoInstitucional,
            @RequestParam(required = false) Integer estatusId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(alumnoService.findAll(page, size, search, curp, correoInstitucional, estatusId, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(alumnoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> create(@RequestBody AlumnoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alumnoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> update(@PathVariable Integer id,
                                                  @RequestBody AlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        alumnoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<AlumnoResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(alumnoService.restore(id));
    }
}
