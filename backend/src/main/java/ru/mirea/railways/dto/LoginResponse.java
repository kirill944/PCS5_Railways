package ru.mirea.railways.dto;
public record LoginResponse(String accessToken, String refreshToken, long expiresInSeconds, boolean mustChangePassword, UserDto user) {}