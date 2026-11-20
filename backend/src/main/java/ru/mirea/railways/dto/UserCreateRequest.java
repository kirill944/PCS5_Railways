package ru.mirea.railways.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record UserCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Pattern(regexp = "ROLE_VIEWER|ROLE_EDITOR|ROLE_ADMIN") String role
) {}