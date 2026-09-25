package ru.mirea.railway.ui;

import static ru.mirea.railway.util.Ansi.*;
import static ru.mirea.railway.util.Gradient.*;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.util.InputValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с пассажирами.
 * Все поля валидируются немедленно при вводе:
 *   - формат полей (regex);
 *   - уникальность паспорта и email — сразу при вводе (БД).
 *
 * Телефон: строго +7XXXXXXXXXX.
 * Дата рождения: в прошлом и не более 150 лет назад.
 */
public class PassengerMenu {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final String DATE_HINT = "dd.MM.yyyy";

    private static final String FULL_NAME_REGEX = "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$";
    private static final String FULL_NAME_ERROR =
            "Неверный формат ФИО. Допускаются буквы, пробелы и дефис (от 3 символов).";

    private static final String PASSPORT_REGEX = "^\\d{4}\\s\\d{6}$";
    private static final String PASSPORT_ERROR =
            "Неверный ввод паспорта. Ожидается формат: серия (4 цифры) номер (6 цифр), "
                    + "например: 1234 123456.";

    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$";
    private static final String EMAIL_ERROR =
            "Неверный ввод email. Ожидается формат 'name@domain.ru'.";

    private static final String PHONE_REGEX = "^\\+7\\d{10}$";
    private static final String PHONE_ERROR =
            "Неверный ввод телефона. Ожидается российский номер в формате '+7XXXXXXXXXX' "
                    + "(знак '+', цифра 7 и ровно 10 цифр).";

    private static final int MAX_AGE = 150;

    private static final int[] PINK = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    private final Scanner scanner;
    private final PassengerService service;

    public PassengerMenu(Scanner scanner, PassengerService service) {
        this.scanner = scanner;
        this.service = service;
    }

    public void show() {
        while (true) {
            printMenu();
            Integer choice = InputValidator.readInt(scanner, between("Выберите действие: ", PINK, VIOLET));
            if (choice == null) {
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> createPassenger();
                    case 2 -> listAll();
                    case 3 -> findById();
                    case 4 -> updatePassenger();
                    case 5 -> deletePassenger();
                    case 6 -> searchByName();
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println(color("⚠ Неизвестный пункт меню", YELLOW));
                }
            } catch (BusinessException e) {
                System.out.println(color("✖ Нарушено правило: " + e.getMessage(), RED));
            } catch (EntityNotFoundException e) {
                System.out.println(color("⚠ " + e.getMessage(), YELLOW));
            } catch (DatabaseException e) {
                System.out.println(color("✖ Ошибка БД: " + e.getMessage(), RED));
            } catch (Exception e) {
                System.out.println(color("✖ Непредвиденная ошибка: " + e.getMessage(), RED));
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println(between("""
                ============ ПАССАЖИРЫ ============
                1. Добавить пассажира
                2. Показать всех
                3. Найти по ID
                4. Редактировать
                5. Удалить
                6. Поиск по ФИО
                0. Назад
                ===================================
                """, PINK, VIOLET));
    }

    private void createPassenger() {
        System.out.println(between("── Создание пассажира ──", PINK, VIOLET));

        String fullName = InputValidator.readRegex(scanner,
                between("ФИО: ", PINK, VIOLET),
                FULL_NAME_REGEX, FULL_NAME_ERROR);

        // Паспорт — сразу проверяем уникальность
        String passport = readUniquePassport(between("Серия и номер паспорта: ", PINK, VIOLET),
                null);

        // Email — сразу проверяем уникальность
        String email = readUniqueEmail(between("Email: ", PINK, VIOLET), null);

        String phone = InputValidator.readRegex(scanner,
                between("Телефон (+7XXXXXXXXXX): ", PINK, VIOLET),
                PHONE_REGEX, PHONE_ERROR);

        LocalDate birth = readBirthDate();

        Passenger p = new Passenger(fullName, passport, email, phone, birth);
        Passenger saved = service.create(p);
        System.out.println(between("✔ Пассажир создан с ID=" + saved.getId(), PINK, VIOLET));
    }

    private void listAll() {
        List<Passenger> list = service.findAll();
        TablePrinter.printPassengers(list);
    }

    private void findById() {
        Long id = InputValidator.readLong(scanner, between("ID пассажира: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        Passenger p = service.findById(id);
        TablePrinter.printPassengers(List.of(p));
    }

    private void updatePassenger() {
        Long id = InputValidator.readLong(scanner,
                between("ID пассажира для редактирования: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        Passenger existing = service.findById(id);

        System.out.println(between("Текущее ФИО: " + existing.getFullName(), PINK, VIOLET));

        String fullName = InputValidator.readRegex(scanner,
                between("Новое ФИО: ", PINK, VIOLET),
                FULL_NAME_REGEX, FULL_NAME_ERROR);

        // excludeId = id — чтобы не конфликтовать с самим собой
        String passport = readUniquePassport(
                between("Новый паспорт (серия 4 цифры + номер 6 цифр): ", PINK, VIOLET),
                id);

        String email = readUniqueEmail(
                between("Новый email: ", PINK, VIOLET),
                id);

        String phone = InputValidator.readRegex(scanner,
                between("Новый телефон (+7XXXXXXXXXX): ", PINK, VIOLET),
                PHONE_REGEX, PHONE_ERROR);

        LocalDate birth = readBirthDate();

        existing.setFullName(fullName);
        existing.setPassportNumber(passport);
        existing.setEmail(email);
        existing.setPhone(phone);
        existing.setBirthDate(birth);

        service.update(existing);
        System.out.println(between("✔ Пассажир обновлён", PINK, VIOLET));
    }

    private void deletePassenger() {
        Long id = InputValidator.readLong(scanner,
                between("ID пассажира для удаления: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        service.findById(id);

        if (!InputValidator.confirm(scanner,
                between("Удалить пассажира и все его брони?", PINK, VIOLET))) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }
        service.delete(id);
        System.out.println(between("✔ Пассажир удалён", PINK, VIOLET));
    }

    private void searchByName() {
        String fragment = InputValidator.readNonEmptyString(scanner,
                between("Фрагмент ФИО: ", PINK, VIOLET));
        List<Passenger> found = service.searchByFullName(fragment);
        TablePrinter.printPassengers(found);
    }

    // =========================================================
    //  Немедленные проверки уникальности через БД
    // =========================================================

    /**
     * Читает паспорт, сразу проверяя формат и уникальность.
     * excludeId — id текущего пассажира при редактировании
     * (чтобы не конфликтовать с самим собой).
     */
    private String readUniquePassport(String prompt, Long excludeId) {
        return InputValidator.readValidated(scanner, prompt, s -> {
            if (s.isEmpty()) {
                return "Поле не может быть пустым.";
            }
            if (!s.matches(PASSPORT_REGEX)) {
                return PASSPORT_ERROR;
            }
            // Идём в БД и проверяем занятость паспорта
            try {
                var existing = service.findByPassport(s);
                if (existing.isPresent()
                        && !existing.get().getId().equals(excludeId)) {
                    return "Пассажир с паспортом '" + s + "' уже существует в базе.";
                }
            } catch (DatabaseException e) {
                return "Не удалось проверить паспорт в БД: " + e.getMessage();
            }
            return null;
        });
    }

    /** Читает email, сразу проверяя формат и уникальность. */
    private String readUniqueEmail(String prompt, Long excludeId) {
        return InputValidator.readValidated(scanner, prompt, s -> {
            if (s.isEmpty()) {
                return "Поле не может быть пустым.";
            }
            if (!s.matches(EMAIL_REGEX)) {
                return EMAIL_ERROR;
            }
            try {
                var existing = service.findByEmail(s);
                if (existing.isPresent()
                        && !existing.get().getId().equals(excludeId)) {
                    return "Пассажир с email '" + s + "' уже существует в базе.";
                }
            } catch (DatabaseException e) {
                return "Не удалось проверить email в БД: " + e.getMessage();
            }
            return null;
        });
    }

    /** Дата рождения: формат dd.MM.yyyy + в прошлом + не старше 150 лет. */
    private LocalDate readBirthDate() {
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(MAX_AGE);

        String line = InputValidator.readValidated(scanner,
                between("Дата рождения (" + DATE_HINT + "): ", PINK, VIOLET), s -> {
                    try {
                        LocalDate d = LocalDate.parse(s, DATE_FMT);
                        if (!d.isBefore(today)) {
                            return "Дата рождения должна быть в прошлом.";
                        }
                        if (d.isBefore(minDate)) {
                            return "Возраст не может превышать " + MAX_AGE
                                    + " лет (дата не раньше "
                                    + minDate.format(DATE_FMT) + ").";
                        }
                        return null;
                    } catch (Exception e) {
                        return "Неверный формат даты. Ожидается " + DATE_HINT + ".";
                    }
                });
        return LocalDate.parse(line, DATE_FMT);
    }
}