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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с бронированиями.
 * Все поля валидируются немедленно при вводе.
 */
public class BookingMenu {

    private static final String STATION_REGEX = "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$";
    private static final String STATION_ERROR =
            "Неверное название станции. Допускаются буквы, пробелы и дефис (от 3 символов).";

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

        String train = InputValidator.readRegex(scanner,
                "Номер поезда (3 цифры + заглавная русская буква, напр. 123А): ",
                InputValidator.TRAIN_REGEX,
                InputValidator.TRAIN_ERROR);

        String from = InputValidator.readRegex(scanner,
                "Станция отправления: ", STATION_REGEX, STATION_ERROR);

        String to = readStationDifferentFrom(from);

        LocalDate date = InputValidator.readDepartureDate(scanner, "Дата отправления");
        LocalTime time = InputValidator.readTimeRequired(scanner, "Время отправления");

        int wagon = InputValidator.readWagonNumber(scanner, "Номер вагона (1–20): ");
        int seat  = InputValidator.readSeatNumber(scanner, "Номер места (1–50): ");
        BigDecimal price = InputValidator.readPrice(scanner, "Цена билета (1000–100000): ");

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

        String train = InputValidator.readRegex(scanner,
                "Номер поезда (3 цифры + заглавная русская буква): ",
                InputValidator.TRAIN_REGEX,
                InputValidator.TRAIN_ERROR);

        String from = InputValidator.readRegex(scanner,
                "Станция отправления: ", STATION_REGEX, STATION_ERROR);

        String to = readStationDifferentFrom(from);

        LocalDate date = InputValidator.readDepartureDate(scanner, "Дата отправления");
        LocalTime time = InputValidator.readTimeRequired(scanner, "Время отправления");

        int wagon = InputValidator.readWagonNumber(scanner, "Номер вагона (1–20): ");
        int seat  = InputValidator.readSeatNumber(scanner, "Номер места (1–50): ");
        BigDecimal price = InputValidator.readPrice(scanner, "Цена (1000–100000): ");

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

        Booking booking = service.findById(id);
        BookingStatus current = booking.getStatus();

        System.out.println();
        System.out.println("Текущий статус: " + current.name()
                + " (" + current.getDisplayName() + ")");

        List<BookingStatus> available = new ArrayList<>();
        for (BookingStatus s : BookingStatus.values()) {
            if (current.canTransitionTo(s)) {
                available.add(s);
            }
        }

        if (available.isEmpty()) {
            System.out.println("⚠ Из текущего статуса нет доступных переходов.");
            System.out.println("  Бронь завершена или отменена — изменить статус нельзя.");
            return;
        }

        System.out.println();
        System.out.println("Доступные переходы:");
        for (int i = 0; i < available.size(); i++) {
            BookingStatus s = available.get(i);
            System.out.printf("  %d. %s (%s)%n", i + 1, s.name(), s.getDisplayName());
        }
        System.out.println("  0. Отмена");

        Integer choice = InputValidator.readInt(scanner, "Выберите действие: ");
        if (choice == null) {
            return;
        }
        if (choice == 0) {
            System.out.println("Отменено");
            return;
        }
        if (choice < 1 || choice > available.size()) {
            System.out.println("⚠ Неверный выбор. Допустимо: 0–" + available.size());
            return;
        }

        BookingStatus newStatus = available.get(choice - 1);

        if (!InputValidator.confirm(scanner,
                "Сменить статус с " + current.name() + " на " + newStatus.name() + "?")) {
            System.out.println("Отменено");
            return;
        }

        service.changeStatus(id, newStatus);
        System.out.println("✔ Статус изменён: " + current.name()
                + " → " + newStatus.name());
    }

    /** Станция назначения, отличная от станции отправления. */
    private String readStationDifferentFrom(String from) {
        return InputValidator.readValidated(scanner,
                "Станция назначения: ", s -> {
                    if (s.isEmpty()) {
                        return "Поле не может быть пустым.";
                    }
                    if (!s.matches(STATION_REGEX)) {
                        return STATION_ERROR;
                    }
                    if (s.equalsIgnoreCase(from)) {
                        return "Станция назначения не может совпадать со станцией отправления.";
                    }
                    return null;
                });
    }
}