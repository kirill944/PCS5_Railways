package ru.mirea.railway.ui;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.util.InputValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с пассажирами.
 * Все поля валидируются немедленно при вводе.
 *
 * Телефон: строго +7XXXXXXXXXX.
 * Дата рождения: в прошлом и не более 150 лет назад.
 */
public class PassengerMenu {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Телефон: строго +7 и ровно 10 цифр. */
    private static final String PHONE_REGEX = "^\\+7\\d{10}$";
    private static final String PHONE_ERROR =
            "Неверный ввод телефона. Ожидается российский номер в формате '+7XXXXXXXXXX' "
                    + "(знак '+', цифра 7 и ровно 10 цифр).";

    /** Максимальный возраст пассажира. */
    private static final int MAX_AGE = 150;

    private final Scanner scanner;
    private final PassengerService service;

    public PassengerMenu(Scanner scanner, PassengerService service) {
        this.scanner = scanner;
        this.service = service;
    }

    public void show() {
        while (true) {
            printMenu();
            Integer choice = InputValidator.readInt(scanner, "Выберите действие: ");
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
                    default -> System.out.println("⚠ Неизвестный пункт меню");
                }
            } catch (BusinessException e) {
                System.out.println("✖ Нарушено правило: " + e.getMessage());
            } catch (EntityNotFoundException e) {
                System.out.println("⚠ " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("✖ Ошибка БД: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("✖ Непредвиденная ошибка: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("""
                ============ ПАССАЖИРЫ ============
                1. Добавить пассажира
                2. Показать всех
                3. Найти по ID
                4. Редактировать
                5. Удалить
                6. Поиск по ФИО
                0. Назад
                ===================================
                """);
    }

    private void createPassenger() {
        System.out.println("── Создание пассажира ──");

        String fullName = InputValidator.readRegex(scanner, "ФИО: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверный формат ФИО. Допускаются буквы, пробелы и дефис (от 3 символов).");

        String passport = InputValidator.readRegex(scanner, "Серия и номер паспорта: ",
                "^\\d{4}\\s\\d{6}$",
                "Неверный ввод паспорта. Ожидается формат: серия (4 цифры) номер (6 цифр), например: 1234 123456.");

        String email = InputValidator.readRegex(scanner, "Email: ",
                "^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$",
                "Неверный ввод email. Ожидается формат 'name@domain.ru'.");

        String phone = InputValidator.readRegex(scanner, "Телефон (+7XXXXXXXXXX): ",
                PHONE_REGEX, PHONE_ERROR);

        LocalDate birth = readBirthDate();

        Passenger p = new Passenger(fullName, passport, email, phone, birth);
        Passenger saved = service.create(p);
        System.out.println("✔ Пассажир создан с ID=" + saved.getId());
    }

    private void listAll() {
        List<Passenger> list = service.findAll();
        TablePrinter.printPassengers(list);
    }

    private void findById() {
        Long id = InputValidator.readLong(scanner, "ID пассажира: ");
        if (id == null) {
            return;
        }
        Passenger p = service.findById(id);
        TablePrinter.printPassengers(List.of(p));
    }

    private void updatePassenger() {
        Long id = InputValidator.readLong(scanner, "ID пассажира для редактирования: ");
        if (id == null) {
            return;
        }
        Passenger existing = service.findById(id);

        System.out.println("Текущее ФИО: " + existing.getFullName());

        String fullName = InputValidator.readRegex(scanner, "Новое ФИО: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверный формат ФИО.");

        String passport = InputValidator.readRegex(scanner, "Новый паспорт (серия (4 цифры) номер (6 цифр)): ",
                "^\\d{4}\\s\\d{6}$",
                "Неверный ввод паспорта. Ожидается формат: серия (4 цифры) номер (6 цифр), например: 1234 123456.");

        String email = InputValidator.readRegex(scanner, "Новый email: ",
                "^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$",
                "Неверный ввод email.");

        String phone = InputValidator.readRegex(scanner, "Новый телефон (+7XXXXXXXXXX): ",
                PHONE_REGEX, PHONE_ERROR);

        LocalDate birth = readBirthDate();

        existing.setFullName(fullName);
        existing.setPassportNumber(passport);
        existing.setEmail(email);
        existing.setPhone(phone);
        existing.setBirthDate(birth);

        service.update(existing);
        System.out.println("✔ Пассажир обновлён");
    }

    private void deletePassenger() {
        Long id = InputValidator.readLong(scanner, "ID пассажира для удаления: ");
        if (id == null) {
            return;
        }
        service.findById(id);

        if (!InputValidator.confirm(scanner, "Удалить пассажира и все его брони?")) {
            System.out.println("Отменено");
            return;
        }
        service.delete(id);
        System.out.println("✔ Пассажир удалён");
    }

    private void searchByName() {
        String fragment = InputValidator.readNonEmptyString(scanner, "Фрагмент ФИО: ");
        List<Passenger> found = service.searchByFullName(fragment);
        TablePrinter.printPassengers(found);
    }

    /**
     * Дата рождения:
     *   - формат dd.MM.yyyy;
     *   - строго в прошлом;
     *   - возраст не более 150 лет.
     */
    private LocalDate readBirthDate() {
        LocalDate minDate = LocalDate.now().minusYears(MAX_AGE);

        String line = InputValidator.readValidated(scanner,
                "Дата рождения (dd.MM.yyyy): ", s -> {
                    try {
                        LocalDate d = LocalDate.parse(s, DATE_FMT);

                        if (!d.isBefore(LocalDate.now())) {
                            return "Дата рождения должна быть в прошлом.";
                        }
                        if (d.isBefore(minDate)) {
                            return "Возраст не может превышать " + MAX_AGE
                                    + " лет (дата не раньше " + minDate.format(DATE_FMT) + ").";
                        }
                        return null;
                    } catch (Exception e) {
                        return "Неверный формат даты. Ожидается dd.MM.yyyy.";
                    }
                });
        return LocalDate.parse(line, DATE_FMT);
    }
}