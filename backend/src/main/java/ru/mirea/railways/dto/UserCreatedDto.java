package ru.mirea.railways.dto;

import java.util.UUID;

/**
 * Ответ на создание учётной записи.
 *
 * Отличается от UserDto наличием temporaryPassword: пароль генерируется
 * на сервере и без этого поля был бы неизвестен даже создавшему пользователя
 * администратору — войти под новой учёткой сразу было невозможно,
 * требовался отдельный вызов reset-password.
 */
public record UserCreatedDto(
        UUID id,
        String email,
        String fullName,
        String role,
        boolean enabled,
        boolean mustChangePassword,
        String temporaryPassword
) {}