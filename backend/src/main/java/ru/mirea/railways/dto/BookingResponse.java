package ru.mirea.railways.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String externalCode,
        UUID passengerId,
        String passengerName,
        String trainNumber,
        String routeFrom,
        String routeTo,
        LocalDate departureDate,
        LocalTime departureTime,
        int wagonNumber,
        int seatNumber,
        BigDecimal price,
        String status,
        String riskLevel,
        boolean paid,
        boolean archived,
        Set<String> tags,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}