package ru.mirea.railway.exception;

import java.sql.SQLException;

/**
 * Обёртка над SQLException.
 *
 * Позволяет:
 *   - отделить ошибки БД от ошибок бизнес-логики;
 *   - скрыть детали JDBC от слоёв выше (Service, UI);
 *   - централизованно обрабатывать ошибки подключения и SQL-запросов.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }

    /** Удобный метод для получения исходного SQLException. */
    public SQLException getSqlException() {
        Throwable cause = getCause();
        return (cause instanceof SQLException) ? (SQLException) cause : null;
    }
}