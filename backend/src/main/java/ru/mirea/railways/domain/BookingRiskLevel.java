package ru.mirea.railways.domain;

import ru.mirea.railways.error.BusinessException;

import java.util.Locale;

public enum BookingRiskLevel {
    LOW, MEDIUM, HIGH;

    /** Разбор уровня риска из query-параметра без IllegalArgumentException (500). */
    public static BookingRiskLevel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Неизвестный уровень риска «" + raw.trim()
                    + "». Допустимые значения: LOW, MEDIUM, HIGH.");
        }
    }
}
