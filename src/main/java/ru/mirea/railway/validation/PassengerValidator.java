package ru.mirea.railway.validation;

import ru.mirea.railway.model.Passenger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Проверка данных пассажира перед сохранением.
 *
 * Правила (согласованы со схемой БД, см. database/schema.sql):
 *   - ФИО: 3–150 символов, только буквы, пробелы и дефис;
 *   - паспорт: «1234 567890» (серия 4 цифры, пробел, номер 6 цифр);
 *   - email: корректный адрес, не длиннее 100 символов;
 *   - телефон: строго +7XXXXXXXXXX;
 *   - дата рождения: в прошлом, возраст не более 150 лет.
 *
 * Уникальность паспорта и email проверяется в PassengerService —
 * для этого нужен доступ к БД.
 */
public final class PassengerValidator {

    public static final String FULL_NAME  = "ФИО";
    public static final String PASSPORT   = "Паспорт";
    public static final String EMAIL      = "Email";
    public static final String PHONE      = "Телефон";
    public static final String BIRTH_DATE = "Дата рождения";

    public static final int FULL_NAME_MIN = 3;
    public static final int FULL_NAME_MAX = 150;
    public static final int EMAIL_MAX     = 100;
    public static final int MAX_AGE       = 150;

    private static final String FULL_NAME_REGEX = "^[А-Яа-яЁёA-Za-z\\s-]+$";
    private static final String PASSPORT_REGEX  = "^\\d{4} \\d{6}$";
    private static final String EMAIL_REGEX     = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-zА-Яа-я]{2,}$";
    private static final String PHONE_REGEX     = "^\\+7\\d{10}$";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private PassengerValidator() {}

    /** Проверяет пассажира и бросает ValidationException со всеми ошибками. */
    public static void validate(Passenger p) {
        ValidationErrors errors = new ValidationErrors();
        validate(p, errors);
        errors.throwIfAny();
    }

    /** Проверяет пассажира, добавляя найденные ошибки в errors. */
    public static void validate(Passenger p, ValidationErrors errors) {
        validateFullName(p.getFullName(), errors);
        validatePassport(p.getPassportNumber(), errors);
        validateEmail(p.getEmail(), errors);
        validatePhone(p.getPhone(), errors);
        validateBirthDate(p.getBirthDate(), errors);
    }

    private static void validateFullName(String value, ValidationErrors errors) {
        if (isBlank(value)) {
            errors.add(FULL_NAME, "обязательное поле — введите фамилию, имя и отчество.");
        } else if (value.length() < FULL_NAME_MIN || value.length() > FULL_NAME_MAX) {
            errors.add(FULL_NAME, "длина должна быть от " + FULL_NAME_MIN + " до " + FULL_NAME_MAX
                    + " символов (сейчас " + value.length() + ").");
        } else if (!value.matches(FULL_NAME_REGEX)) {
            errors.add(FULL_NAME, "допускаются только буквы, пробелы и дефис. "
                    + "Пример: Иванов Иван Иванович.");
        }
    }

    private static void validatePassport(String value, ValidationErrors errors) {
        if (isBlank(value)) {
            errors.add(PASSPORT, "обязательное поле — введите серию и номер паспорта.");
        } else if (!value.matches(PASSPORT_REGEX)) {
            errors.add(PASSPORT, "ожидается серия (4 цифры), пробел и номер (6 цифр). "
                    + "Пример: 4510 123456.");
        }
    }

    private static void validateEmail(String value, ValidationErrors errors) {
        if (isBlank(value)) {
            errors.add(EMAIL, "обязательное поле — введите адрес электронной почты.");
        } else if (value.length() > EMAIL_MAX) {
            errors.add(EMAIL, "адрес не может быть длиннее " + EMAIL_MAX
                    + " символов (сейчас " + value.length() + ").");
        } else if (!value.matches(EMAIL_REGEX)) {
            errors.add(EMAIL, "некорректный адрес. Пример: ivanov@mail.ru.");
        }
    }

    private static void validatePhone(String value, ValidationErrors errors) {
        if (isBlank(value)) {
            errors.add(PHONE, "обязательное поле — введите номер телефона.");
        } else if (!value.matches(PHONE_REGEX)) {
            errors.add(PHONE, "ожидается «+7» и ровно 10 цифр без пробелов и скобок. "
                    + "Пример: +79161234567.");
        }
    }

    private static void validateBirthDate(LocalDate value, ValidationErrors errors) {
        if (value == null) {
            errors.add(BIRTH_DATE, "обязательное поле — выберите дату в календаре.");
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(MAX_AGE);
        if (!value.isBefore(today)) {
            errors.add(BIRTH_DATE, "дата рождения должна быть в прошлом (указано "
                    + value.format(DATE_FMT) + ").");
        } else if (value.isBefore(minDate)) {
            errors.add(BIRTH_DATE, "возраст не может превышать " + MAX_AGE
                    + " лет — укажите дату не раньше " + minDate.format(DATE_FMT) + ".");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
