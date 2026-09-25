package ru.mirea.railway.exception;

/**
 * Выбрасывается при нарушении бизнес-правил предметной области.
 *
 * Примеры:
 *   - попытка забронировать уже занятое место;
 *   - запрещённый переход статуса;
 *   - дата отправления в прошлом;
 *   - цена билета <= 0.
 *
 * Реализовано как unchecked-исключение, чтобы не «засорять»
 * сигнатуры методов сервисного слоя.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}