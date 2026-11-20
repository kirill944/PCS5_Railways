package ru.mirea.railways.security;

import org.springframework.stereotype.Component;
import ru.mirea.railways.error.BusinessException;

@Component
public class PasswordPolicy {
    public void validate(String raw) {
        if (raw == null || raw.length() < 8) throw new BusinessException("Пароль должен содержать минимум 8 символов");
        boolean upper = raw.chars().anyMatch(Character::isUpperCase);
        boolean lower = raw.chars().anyMatch(Character::isLowerCase);
        boolean digit = raw.chars().anyMatch(Character::isDigit);
        if (!(upper && lower && digit)) {
            throw new BusinessException("Пароль должен содержать буквы верхнего и нижнего регистра и цифры");
        }
    }
}