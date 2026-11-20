package ru.mirea.railways.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mirea.railways.dto.PassengerDto;
import ru.mirea.railways.dto.PassengerRequest;
import ru.mirea.railways.service.PassengerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {
    private final PassengerService service;
    public PassengerController(PassengerService service) { this.service = service; }

    @GetMapping
    public List<PassengerDto> list() { return service.list(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PassengerDto> create(@Valid @RequestBody PassengerRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PassengerDto update(@PathVariable UUID id, @Valid @RequestBody PassengerRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggle(@PathVariable UUID id, @RequestParam boolean active) {
        service.toggle(id, active); return ResponseEntity.noContent().build();
    }
}