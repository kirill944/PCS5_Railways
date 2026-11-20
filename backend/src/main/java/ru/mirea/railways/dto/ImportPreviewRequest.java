package ru.mirea.railways.dto;
import jakarta.validation.constraints.NotNull;
public record ImportPreviewRequest(@NotNull String csvContent, @NotNull String mode) {}