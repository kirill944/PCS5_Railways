package ru.mirea.railway.exception;

import ru.mirea.railway.validation.FieldError;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Введённые данные не прошли проверку.
 * Содержит список ошибок по конкретным полям, чтобы интерфейс
 * мог показать, какое поле заполнено неверно и что исправить.
 */
public class ValidationException extends BusinessException {

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super(format(errors));
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String field, String message) {
        this(List.of(new FieldError(field, message)));
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    private static String format(List<FieldError> errors) {
        return errors.stream()
                .map(e -> "«" + e.field() + "»: " + e.message())
                .collect(Collectors.joining("\n"));
    }
}
