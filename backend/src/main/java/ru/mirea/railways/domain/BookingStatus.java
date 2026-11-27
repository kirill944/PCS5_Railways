package ru.mirea.railways.domain;

import ru.mirea.railways.error.BusinessException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum BookingStatus {
    CREATED, CONFIRMED, PAID, COMPLETED, CANCELLED;

    /**
     * Разбор статуса из запроса. Пустое/отсутствующее значение — null,
     * неизвестное — понятная ошибка вместо IllegalArgumentException (500).
     */
    public static BookingStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Неизвестный статус «" + raw.trim()
                    + "». Допустимые значения: " + names() + ".");
        }
    }

    public static String names() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}