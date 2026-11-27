package ru.mirea.railways.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mirea.railways.domain.AuditEvent;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.repository.AuditEventRepository;
import ru.mirea.railways.service.UserAdminService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserAdminService users;
    private final AuditEventRepository audit;

    public AdminController(UserAdminService users, AuditEventRepository audit) {
        this.users = users; this.audit = audit;
    }

    @GetMapping("/users")
    public PageResponse<UserDto> listUsers(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return users.list(page, size);
    }

    @PostMapping("/users")
    public ResponseEntity<UserCreatedDto> createUser(@Valid @RequestBody UserCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(r));
    }

    @PutMapping("/users/{id}")
    public UserDto updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest r) {
        return users.update(id, r);
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable UUID id) {
        return Map.of("temporaryPassword", users.resetPassword(id));
    }

    @GetMapping("/audit")
    public PageResponse<AuditEventDto> audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType
    ) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        // Параметр eventType объявлен давно, но не применялся — фильтр игнорировался
        Page<AuditEvent> result = (eventType == null || eventType.isBlank())
                ? audit.findAll(p)
                : audit.findByEventType(eventType, p);
        return new PageResponse<>(result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext());
    }

    private AuditEventDto toDto(AuditEvent e) {
        return new AuditEventDto(e.getId(), e.getActorEmail(), e.getEventType(),
                e.getEntityType(), e.getEntityId(), e.getCorrelationId(),
                e.getDescription(), e.getCreatedAt());
    }
}