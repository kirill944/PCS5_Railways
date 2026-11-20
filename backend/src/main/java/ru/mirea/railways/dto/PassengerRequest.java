package ru.mirea.railways.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record PassengerRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 20) String passportNumber,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotNull @Past LocalDate birthDate
) {}