package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.ProfesorGrupoRequest;
import mx.cucii.school.platform.dto.ProfesorGrupoDetalleResponse;
import mx.cucii.school.platform.dto.ProfesorGrupoResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.service.ProfesorGrupoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profesores-grupos")
@RequiredArgsConstructor
public class ProfesorGrupoController {

    private final ProfesorGrupoService profesorGrupoService;

    @GetMapping
    public ResponseEntity<PageResponse<ProfesorGrupoDetalleResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer grupoId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(profesorGrupoService.findAll(page, size, usuarioId, grupoId, materiaId, isActive, search, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfesorGrupoResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(profesorGrupoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProfesorGrupoResponse> create(@RequestBody ProfesorGrupoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesorGrupoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfesorGrupoResponse> update(@PathVariable Integer id,
                                                         @RequestBody ProfesorGrupoRequest request) {
        return ResponseEntity.ok(profesorGrupoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        profesorGrupoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
