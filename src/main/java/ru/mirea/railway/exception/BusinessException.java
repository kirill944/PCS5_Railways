package ru.mirea.railway.exception;

/**
 * Выбрасывается при нарушении бизнес-правил предметной области.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}