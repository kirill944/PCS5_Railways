package ru.mirea.railways.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record NoteRequest(@NotBlank @Size(max = 2000) String body, boolean pinned) {}