package ru.mirea.railway.exception;

import java.sql.SQLException;

/**
 * Обёртка над SQLException.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public SQLException getSqlException() {
        Throwable cause = getCause();
        return (cause instanceof SQLException) ? (SQLException) cause : null;
    }
}