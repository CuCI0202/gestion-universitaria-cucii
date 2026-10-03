package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.dto.UsuarioRequest;
import mx.cucii.school.platform.dto.UsuarioResponse;
import mx.cucii.school.platform.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer rolId,
            @RequestParam(required = false) Integer plantelId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(usuarioService.findAll(page, size, search, rolId, plantelId, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(@PathVariable Integer id, @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "false") boolean deactivate) {
        usuarioService.delete(id, deactivate);
        String message = deactivate
                ? "Usuario desactivado correctamente"
                : "Usuario eliminado correctamente";
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<UsuarioResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.restore(id));
    }
}
