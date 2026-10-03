package mx.cucii.school.platform.service;

import lombok.RequiredArgsConstructor;
import mx.cucii.school.platform.dto.UsuarioRequest;
import mx.cucii.school.platform.dto.UsuarioResponse;
import mx.cucii.school.platform.dto.PageResponse;
import mx.cucii.school.platform.exception.ResourceNotFoundException;
import mx.cucii.school.platform.model.Rol;
import mx.cucii.school.platform.model.Usuario;
import mx.cucii.school.platform.repository.RolJdbcRepository;
import mx.cucii.school.platform.repository.UsuarioJdbcRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioJdbcRepository usuarioRepository;
    private final RolJdbcRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UsuarioResponse> findAll(int page, int size, String search,
                                                 Integer rolId, Integer plantelId,
                                                 Boolean isActive, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa");
        if (size < 1) throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        if (size > 100) throw new IllegalArgumentException("El tamaño de página no puede ser mayor a 100");

        long totalElements = usuarioRepository.countFiltered(search, rolId, plantelId, isActive);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int offset = page * size;

        List<UsuarioResponse> content = usuarioRepository.findAll(size, offset, search, rolId, plantelId, isActive, sortBy, sortDir).stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }

    public UsuarioResponse findById(Integer id) {
        return usuarioRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("El email ya está registrado");
        });
        OffsetDateTime now = OffsetDateTime.now();
        Usuario nuevo = new Usuario(
                null,
                request.nombre(),
                request.apellido(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.rolId(),
                request.plantelId(),
                true,
                now,
                now
        );
        return toResponse(usuarioRepository.save(nuevo));
    }

    @Transactional
    public UsuarioResponse update(Integer id, UsuarioRequest request) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        if (!existing.email().equals(request.email())) {
            usuarioRepository.findByEmail(request.email()).ifPresent(u -> {
                throw new IllegalArgumentException("El email ya está registrado");
            });
        }
        String passwordHash = (request.password() != null && !request.password().isBlank())
                ? passwordEncoder.encode(request.password())
                : existing.passwordHash();
        Usuario updated = new Usuario(
                existing.id(),
                request.nombre(),
                request.apellido(),
                request.email(),
                passwordHash,
                request.rolId(),
                request.plantelId(),
                existing.isActive(),
                existing.createdAt(),
                OffsetDateTime.now()
        );
        return toResponse(usuarioRepository.save(updated));
    }

    @Transactional
    public void delete(Integer id, boolean deactivate) {
        if (!usuarioRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + id);
        }
        if (deactivate) {
            usuarioRepository.softDeleteById(id, OffsetDateTime.now());
        } else {
            usuarioRepository.deleteById(id);
        }
    }

    @Transactional
    public UsuarioResponse restore(Integer id) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        if (!existing.isActive()) {
            usuarioRepository.restoreById(id, OffsetDateTime.now());
        }
        return toResponse(usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id)));
    }

    private UsuarioResponse toResponse(Usuario u) {
        String rolNombre = rolRepository.findById(u.rolId())
                .map(Rol::nombre)
                .orElse("");
        return new UsuarioResponse(
                u.id(), u.nombre(), u.apellido(), u.email(),
                u.rolId(), rolNombre,
                u.plantelId(), u.isActive(),
                u.createdAt(), u.updatedAt()
        );
    }
}
