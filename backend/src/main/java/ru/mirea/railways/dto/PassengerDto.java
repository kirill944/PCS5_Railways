package ru.mirea.railways.dto;
import java.time.LocalDate;
import java.util.UUID;
public record PassengerDto(UUID id, String fullName, String passportNumber, String email,
                           String phone, LocalDate birthDate, boolean active) {}