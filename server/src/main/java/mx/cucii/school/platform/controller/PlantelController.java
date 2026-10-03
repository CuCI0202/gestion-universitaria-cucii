package mx.cucii.school.platform.controller;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.dto.PlantelRequest;
import mx.cucii.school.platform.dto.PlantelResponse;
import mx.cucii.school.platform.service.PlantelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/planteles")
@RequiredArgsConstructor
public class PlantelController {

    private final PlantelService plantelService;

    @GetMapping
    public ResponseEntity<PageResponse<PlantelResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(plantelService.findAll(page, size, search, estado, pais, isActive, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantelResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(plantelService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PlantelResponse> create(@RequestBody PlantelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantelService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantelResponse> update(@PathVariable Integer id,
                                                   @RequestBody PlantelRequest request) {
        return ResponseEntity.ok(plantelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        plantelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<PlantelResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(plantelService.restore(id));
    }
}
