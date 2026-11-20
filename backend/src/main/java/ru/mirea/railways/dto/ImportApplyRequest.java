package ru.mirea.railways.dto;
import jakarta.validation.constraints.NotBlank;
public record ImportApplyRequest(@NotBlank String token) {}