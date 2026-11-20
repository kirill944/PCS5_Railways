package ru.mirea.railways.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record BookingRequest(
        @NotNull UUID passengerId,
        @NotBlank @Size(max = 10) String trainNumber,
        @NotBlank @Size(max = 100) String routeFrom,
        @NotBlank @Size(max = 100) String routeTo,
        @NotNull @FutureOrPresent LocalDate departureDate,
        @NotNull LocalTime departureTime,
        @Min(1) int wagonNumber,
        @Min(1) int seatNumber,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        String status,
        boolean paid,
        Set<String> tags,
        Long version
) {}