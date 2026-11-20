package ru.mirea.railways.dto;
import java.time.Instant;
import java.util.UUID;
public record NoteDto(UUID id, UUID bookingId, String authorEmail, String body, boolean pinned,
                      Instant createdAt, Instant updatedAt) {}