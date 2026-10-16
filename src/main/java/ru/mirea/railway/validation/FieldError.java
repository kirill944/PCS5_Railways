package ru.mirea.railway.validation;

/**
 * Ошибка проверки одного поля: название поля (как его видит пользователь)
 * и текст — что именно не так и как это исправить.
 */
public record FieldError(String field, String message) {

    @Override
    public String toString() {
        return field + ": " + message;
    }
}
