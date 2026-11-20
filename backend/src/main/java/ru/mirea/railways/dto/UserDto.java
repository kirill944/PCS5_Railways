package ru.mirea.railways.dto;
import java.util.UUID;
public record UserDto(UUID id, String email, String fullName, String role,
                      boolean enabled, boolean mustChangePassword) {}