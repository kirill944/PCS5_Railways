package ru.mirea.railway.model;

/**
 * Статус бронирования железнодорожного билета.
 *
 * Жизненный цикл брони:
 *   CREATED -> CONFIRMED -> PAID -> COMPLETED
 *   любой статус (кроме COMPLETED и CANCELLED) -> CANCELLED
 */
public enum BookingStatus {

    CREATED("Создана"),
    CONFIRMED("Подтверждена"),
    PAID("Оплачена"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Проверяет, допустим ли переход из текущего статуса в новый.
     * Используется в слое service при смене статуса брони.
     */
    public boolean canTransitionTo(BookingStatus next) {
        if (next == null || next == this) {
            return false;
        }
        return switch (this) {
            case CREATED   -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PAID      || next == CANCELLED;
            case PAID      -> next == COMPLETED || next == CANCELLED;
            case COMPLETED -> false;   // из завершённой — никуда
            case CANCELLED -> false;   // из отменённой — никуда
        };
    }

    /**
     * Безопасный парсинг строки в enum.
     * Бросает IllegalArgumentException с понятным сообщением,
     * если значение некорректно.
     */
    public static BookingStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Статус не может быть пустым");
        }
        try {
            return BookingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Некорректный статус: '" + value + "'. Допустимые значения: "
                            + java.util.Arrays.toString(BookingStatus.values()));
        }
    }

    @Override
    public String toString() {
        return name() + " (" + displayName + ")";
    }
}