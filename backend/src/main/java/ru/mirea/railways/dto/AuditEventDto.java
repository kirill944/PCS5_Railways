package ru.mirea.railways.dto;
import java.time.Instant;
import java.util.UUID;
public record AuditEventDto(UUID id, String actorEmail, String eventType, String entityType,
                            UUID entityId, String correlationId, String description, Instant createdAt) {}