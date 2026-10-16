package ru.mirea.railway.validation;

import ru.mirea.railway.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Накопитель ошибок проверки. Позволяет показать пользователю
 * все неверно заполненные поля сразу, а не по одному.
 *
 * Для каждого поля сохраняется только первая ошибка: если значение
 * не удалось даже разобрать (например, «abc» в поле «Вагон»),
 * дальнейшие проверки диапазона по нему не дублируются.
 */
public final class ValidationErrors {

    private final List<FieldError> errors = new ArrayList<>();

    public void add(String field, String message) {
        if (!hasErrorFor(field)) {
            errors.add(new FieldError(field, message));
        }
    }

    public boolean hasErrorFor(String field) {
        return errors.stream().anyMatch(e -> e.field().equals(field));
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public List<FieldError> list() {
        return List.copyOf(errors);
    }

    /** Бросает ValidationException, если накоплена хотя бы одна ошибка. */
    public void throwIfAny() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
