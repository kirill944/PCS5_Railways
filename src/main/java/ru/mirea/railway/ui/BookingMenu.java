package ru.mirea.railway.ui;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.service.BookingService;
import ru.mirea.railway.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

/**
 * Подменю работы с бронированиями.
 * Все поля валидируются немедленно при вводе.
 */
public class BookingMenu {

    private final Scanner scanner;
    private final BookingService service;

    public BookingMenu(Scanner scanner, BookingService service) {
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
                    case 1 -> createBooking();
                    case 2 -> listAll();
                    case 3 -> findById();
                    case 4 -> updateBooking();
                    case 5 -> deleteBooking();
                    case 6 -> changeStatus();
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
                ========== БРОНИРОВАНИЯ ==========
                1. Создать бронь
                2. Показать все
                3. Найти по ID
                4. Редактировать
                5. Удалить
                6. Сменить статус
                0. Назад
                ==================================
                """);
    }

    private void createBooking() {
        System.out.println("── Создание брони ──");

        long passengerId = InputValidator.readLongRequired(scanner, "ID пассажира: ");

        String train = InputValidator.readRegex(scanner, "Номер поезда (напр. 123А): ",
                "^[0-9]{3}[А-Яа-яA-Za-z]$",
                "Неверный формат номера поезда. Ожидается 3 цифры и буква, например '123А'.");

        String from = InputValidator.readRegex(scanner, "Станция отправления: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции. Допускаются буквы, пробелы и дефис.");

        String to = InputValidator.readRegex(scanner, "Станция назначения: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        LocalDate date = InputValidator.readDateRequired(scanner, "Дата отправления");
        LocalTime time = InputValidator.readTimeRequired(scanner, "Время отправления");

        int wagon = readPositiveInt("Номер вагона: ");
        int seat  = readPositiveInt("Номер места: ");
        BigDecimal price = readPositiveDecimal("Цена билета: ");

        Booking b = new Booking(passengerId, train, from, to, date, time,
                wagon, seat, price, BookingStatus.CREATED);

        Booking saved = service.create(b);
        System.out.println("✔ Бронь создана с ID=" + saved.getId()
                + ", статус: " + saved.getStatus());
    }

    private void listAll() {
        TablePrinter.printBookings(service.findAll());
    }

    private void findById() {
        Long id = InputValidator.readLong(scanner, "ID брони: ");
        if (id == null) {
            return;
        }
        TablePrinter.printSingleBooking(service.findById(id));
    }

    private void updateBooking() {
        Long id = InputValidator.readLong(scanner, "ID брони для редактирования: ");
        if (id == null) {
            return;
        }
        Booking existing = service.findById(id);

        System.out.println("Текущие данные:");
        TablePrinter.printSingleBooking(existing);

        long passengerId = InputValidator.readLongRequired(scanner, "ID пассажира: ");

        String train = InputValidator.readRegex(scanner, "Номер поезда (напр. 123А): ",
                "^[0-9]{3}[А-Яа-яA-Za-z]$",
                "Неверный формат номера поезда.");

        String from = InputValidator.readRegex(scanner, "Станция отправления: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        String to = InputValidator.readRegex(scanner, "Станция назначения: ",
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        LocalDate date = InputValidator.readDateRequired(scanner, "Дата отправления");
        LocalTime time = InputValidator.readTimeRequired(scanner, "Время отправления");

        int wagon = readPositiveInt("Номер вагона: ");
        int seat  = readPositiveInt("Номер места: ");
        BigDecimal price = readPositiveDecimal("Цена: ");

        existing.setPassengerId(passengerId);
        existing.setTrainNumber(train);
        existing.setRouteFrom(from);
        existing.setRouteTo(to);
        existing.setDepartureDate(date);
        existing.setDepartureTime(time);
        existing.setWagonNumber(wagon);
        existing.setSeatNumber(seat);
        existing.setPrice(price);

        service.update(existing);
        System.out.println("✔ Бронь обновлена");
    }

    private void deleteBooking() {
        Long id = InputValidator.readLong(scanner, "ID брони для удаления: ");
        if (id == null) {
            return;
        }
        service.findById(id);
        if (!InputValidator.confirm(scanner, "Удалить бронь?")) {
            System.out.println("Отменено");
            return;
        }
        service.delete(id);
        System.out.println("✔ Бронь удалена");
    }

    private void changeStatus() {
        Long id = InputValidator.readLong(scanner, "ID брони: ");
        if (id == null) {
            return;
        }
        Booking b = service.findById(id);
        System.out.println("Текущий статус: " + b.getStatus());
        System.out.println("Доступные статусы:");
        for (BookingStatus s : BookingStatus.values()) {
            System.out.println("  - " + s.name() + " (" + s.getDisplayName() + ")");
        }

        String input = InputValidator.readValidated(scanner, "Новый статус: ", line -> {
            try {
                BookingStatus.fromString(line);
                return null;
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        });
        BookingStatus newStatus = BookingStatus.fromString(input);

        service.changeStatus(id, newStatus);
        System.out.println("✔ Статус изменён на " + newStatus);
    }

    /** Целое ≥ 1. */
    private int readPositiveInt(String prompt) {
        String line = InputValidator.readValidated(scanner, prompt, s -> {
            try {
                return Integer.parseInt(s) >= 1
                        ? null
                        : "Значение должно быть ≥ 1.";
            } catch (NumberFormatException e) {
                return "Ожидалось целое число.";
            }
        });
        return Integer.parseInt(line);
    }

    /** BigDecimal > 0. */
    private BigDecimal readPositiveDecimal(String prompt) {
        String line = InputValidator.readValidated(scanner, prompt, s -> {
            try {
                return new BigDecimal(s.replace(',', '.')).signum() > 0
                        ? null
                        : "Цена должна быть > 0.";
            } catch (NumberFormatException e) {
                return "Ожидалось число.";
            }
        });
        return new BigDecimal(line.replace(',', '.'));
    }
}