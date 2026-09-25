package ru.mirea.railway.ui;

import static ru.mirea.railway.util.Ansi.*;
import static ru.mirea.railway.util.Gradient.*;

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
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

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

    private static final int[] PINK = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    public void show() {
        while (true) {
            printMenu();
            Integer choice = InputValidator.readInt(scanner, between("Выберите действие: ", PINK, VIOLET));
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
                ========== БРОНИРОВАНИЯ ==========
                1. Создать бронь
                2. Показать все
                3. Найти по ID
                4. Редактировать
                5. Удалить
                6. Сменить статус
                0. Назад
                ==================================
                """, PINK, VIOLET));
    }

    private void createBooking() {
        System.out.println(between("── Создание брони ──", PINK, VIOLET));
        long passengerId = InputValidator.readLongRequired(scanner, between("ID пассажира: ", PINK, VIOLET));

        String train = InputValidator.readRegex(scanner, between("Номер поезда (напр. 123А): ", PINK, VIOLET),
                "^[0-9]{3}[А-Яа-яA-Za-z]$",
                "Неверный формат номера поезда. Ожидается 3 цифры и буква, например '123А'.");

        String from = InputValidator.readRegex(scanner, between("Станция отправления: ", PINK, VIOLET),
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции. Допускаются буквы, пробелы и дефис.");

        String to = InputValidator.readRegex(scanner, between("Станция назначения: ", PINK, VIOLET),
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        LocalDate date = InputValidator.readDateRequired(scanner, between("Дата отправления", PINK, VIOLET));
        LocalTime time = InputValidator.readTimeRequired(scanner, between("Время отправления", PINK, VIOLET));

        int wagon = readPositiveInt(between("Номер вагона: ", PINK, VIOLET));
        int seat  = readPositiveInt(between("Номер места: ", PINK, VIOLET));
        BigDecimal price = readPositiveDecimal(between("Цена билета: ", PINK, VIOLET));

        Booking b = new Booking(passengerId, train, from, to, date, time,
                wagon, seat, price, BookingStatus.CREATED);

        Booking saved = service.create(b);
        System.out.println(between("✔ Бронь создана с ID=" + saved.getId()
                + ", статус: " + saved.getStatus(), PINK, VIOLET));
    }

    private void listAll() {
        TablePrinter.printBookings(service.findAll());
    }

    private void findById() {
        Long id = InputValidator.readLong(scanner, between("ID брони: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        TablePrinter.printSingleBooking(service.findById(id));
    }

    private void updateBooking() {
        Long id = InputValidator.readLong(scanner, between("ID брони для редактирования: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        Booking existing = service.findById(id);

        System.out.println(between("Текущие данные:", PINK, VIOLET));
        TablePrinter.printSingleBooking(existing);

        long passengerId = InputValidator.readLongRequired(scanner, between("ID пассажира: ", PINK, VIOLET));

        String train = InputValidator.readRegex(scanner, between("Номер поезда (напр. 123А): ", PINK, VIOLET),
                "^[0-9]{3}[А-Яа-яA-Za-z]$",
                "Неверный формат номера поезда.");

        String from = InputValidator.readRegex(scanner, between("Станция отправления: ", PINK, VIOLET),
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        String to = InputValidator.readRegex(scanner, between("Станция назначения: ", PINK, VIOLET),
                "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$",
                "Неверное название станции.");

        LocalDate date = InputValidator.readDateRequired(scanner, between("Дата отправления", PINK, VIOLET));
        LocalTime time = InputValidator.readTimeRequired(scanner, between("Время отправления", PINK, VIOLET));

        int wagon = readPositiveInt(between("Номер вагона: ", PINK, VIOLET));
        int seat  = readPositiveInt(between("Номер места: ", PINK, VIOLET));
        BigDecimal price = readPositiveDecimal(between("Цена: ", PINK, VIOLET));

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
        System.out.println(between("✔ Бронь обновлена", PINK, VIOLET));
    }

    private void deleteBooking() {
        Long id = InputValidator.readLong(scanner, between("ID брони для удаления: ", PINK, VIOLET));
        if (id == null) return;
        service.findById(id);
        if (!InputValidator.confirm(scanner, between("Удалить бронь?", PINK, VIOLET))) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }
        service.delete(id);
        System.out.println(between("✔ Бронь удалена", PINK, VIOLET));
    }

    private void changeStatus() {
        Long id = InputValidator.readLong(scanner, between("ID брони: ", PINK, VIOLET));
        if (id == null) return;

        Booking booking = service.findById(id);
        BookingStatus current = booking.getStatus();

        System.out.println();
        System.out.println(between("Текущий статус: " + current.name()
                + " (" + current.getDisplayName() + ")", PINK, VIOLET));

        // Собираем только доступные переходы
        List<BookingStatus> available = new ArrayList<>();
        for (BookingStatus s : BookingStatus.values()) {
            if (current.canTransitionTo(s)) {
                available.add(s);
            }
        }

        if (available.isEmpty()) {
            System.out.println(color("⚠ Из текущего статуса нет доступных переходов.", YELLOW));
            System.out.println(color("  Бронь завершена или отменена — изменить статус нельзя.", YELLOW));
            return;
        }

        System.out.println();
        System.out.println(between("Доступные переходы:", PINK, VIOLET));
        for (int i = 0; i < available.size(); i++) {
            BookingStatus s = available.get(i);
            System.out.println(between(String.format("  %d. %s (%s)", i + 1, s.name(), s.getDisplayName()), PINK, VIOLET));
        }
        System.out.println(between("  0. Отмена", PINK, VIOLET));

        Integer choice = InputValidator.readInt(scanner, between("Выберите действие: ", PINK, VIOLET));
        if (choice == null) return;

        if (choice == 0) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }

        if (choice < 1 || choice > available.size()) {
            System.out.println(color("⚠ Неверный выбор. Допустимо: 0–" + available.size(), YELLOW));
            return;
        }

        BookingStatus newStatus = available.get(choice - 1);

        // Подтверждение
        if (!InputValidator.confirm(scanner, between(
                "Сменить статус с " + current.name() + " на " + newStatus.name() + "?", PINK, VIOLET))) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }

        service.changeStatus(id, newStatus);
        System.out.println(between("✔ Статус изменён: " + current.name()
                + " → " + newStatus.name(), PINK, VIOLET));
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