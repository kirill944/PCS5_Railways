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
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с бронированиями.
 * Все поля валидируются немедленно при вводе:
 *   - дата: не в прошлом и не позже года вперёд;
 *   - станции не совпадают;
 *   - вагон 1–20, место 1–50, цена 1000–100 000 ₽;
 *   - занятость места (поезд+вагон+место+дата) проверяется сразу при вводе.
 */
public class BookingMenu {

    private static final String STATION_REGEX = "^[А-Яа-яЁёA-Za-z\\s-]{3,150}$";
    private static final String STATION_ERROR =
            "Неверное название станции. Допускаются буквы, пробелы и дефис (от 3 символов).";

    private static final int[] PINK = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    private final Scanner scanner;
    private final BookingService service;

    public BookingMenu(Scanner scanner, BookingService service) {
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

        long passengerId = InputValidator.readLongRequired(scanner,
                between("ID пассажира: ", PINK, VIOLET));

        String train = InputValidator.readRegex(scanner,
                between("Номер поезда (3 цифры + заглавная русская буква, напр. 123А): ", PINK, VIOLET),
                InputValidator.TRAIN_REGEX,
                InputValidator.TRAIN_ERROR);

        String from = InputValidator.readRegex(scanner,
                between("Станция отправления: ", PINK, VIOLET),
                STATION_REGEX, STATION_ERROR);

        String to = readStationDifferentFrom(between("Станция назначения: ", PINK, VIOLET), from);

        LocalDate date = InputValidator.readDepartureDate(scanner,
                between("Дата отправления", PINK, VIOLET));
        LocalTime time = InputValidator.readTimeRequired(scanner,
                between("Время отправления", PINK, VIOLET));

        int wagon = InputValidator.readWagonNumber(scanner,
                between("Номер вагона (1–20): ", PINK, VIOLET));

        // Место — сразу проверяем занятость
        int seat = readFreeSeat(between("Номер места (1–50): ", PINK, VIOLET),
                train, wagon, date);

        BigDecimal price = InputValidator.readPrice(scanner,
                between("Цена билета (1000–100000): ", PINK, VIOLET));

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
        Long id = InputValidator.readLong(scanner,
                between("ID брони для редактирования: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
        Booking existing = service.findById(id);

        System.out.println(between("Текущие данные:", PINK, VIOLET));
        TablePrinter.printSingleBooking(existing);

        long passengerId = InputValidator.readLongRequired(scanner,
                between("ID пассажира: ", PINK, VIOLET));

        String train = InputValidator.readRegex(scanner,
                between("Номер поезда (3 цифры + заглавная русская буква): ", PINK, VIOLET),
                InputValidator.TRAIN_REGEX,
                InputValidator.TRAIN_ERROR);

        String from = InputValidator.readRegex(scanner,
                between("Станция отправления: ", PINK, VIOLET),
                STATION_REGEX, STATION_ERROR);

        String to = readStationDifferentFrom(between("Станция назначения: ", PINK, VIOLET), from);

        LocalDate date = InputValidator.readDepartureDate(scanner,
                between("Дата отправления", PINK, VIOLET));
        LocalTime time = InputValidator.readTimeRequired(scanner,
                between("Время отправления", PINK, VIOLET));

        int wagon = InputValidator.readWagonNumber(scanner,
                between("Номер вагона (1–20): ", PINK, VIOLET));

        // Место — сразу проверяем занятость (с учётом, что это та же бронь)
        int seat = readFreeSeatForUpdate(
                between("Номер места (1–50): ", PINK, VIOLET),
                train, wagon, date, existing);

        BigDecimal price = InputValidator.readPrice(scanner,
                between("Цена (1000–100000): ", PINK, VIOLET));

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
        Long id = InputValidator.readLong(scanner,
                between("ID брони для удаления: ", PINK, VIOLET));
        if (id == null) {
            return;
        }
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
        if (id == null) {
            return;
        }

        Booking booking = service.findById(id);
        BookingStatus current = booking.getStatus();

        System.out.println();
        System.out.println(between("Текущий статус: " + current.name()
                + " (" + current.getDisplayName() + ")", PINK, VIOLET));

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
            System.out.println(between(
                    String.format("  %d. %s (%s)", i + 1, s.name(), s.getDisplayName()),
                    PINK, VIOLET));
        }
        System.out.println(between("  0. Отмена", PINK, VIOLET));

        Integer choice = InputValidator.readInt(scanner, between("Выберите действие: ", PINK, VIOLET));
        if (choice == null) {
            return;
        }
        if (choice == 0) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }
        if (choice < 1 || choice > available.size()) {
            System.out.println(color("⚠ Неверный выбор. Допустимо: 0–" + available.size(), YELLOW));
            return;
        }

        BookingStatus newStatus = available.get(choice - 1);

        if (!InputValidator.confirm(scanner, between(
                "Сменить статус с " + current.name() + " на " + newStatus.name() + "?",
                PINK, VIOLET))) {
            System.out.println(between("Отменено", PINK, VIOLET));
            return;
        }

        service.changeStatus(id, newStatus);
        System.out.println(between("✔ Статус изменён: " + current.name()
                + " → " + newStatus.name(), PINK, VIOLET));
    }

    //  Проверки станции и занятости места

    /** Станция назначения, отличная от станции отправления. */
    private String readStationDifferentFrom(String prompt, String from) {
        return InputValidator.readValidated(scanner, prompt, s -> {
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

    /** Место: 1–50 И не занято на поезд/вагон/дату. */
    private int readFreeSeat(String prompt, String train, int wagon, LocalDate date) {
        String line = InputValidator.readValidated(scanner, prompt, s -> {
            int v;
            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return "Ожидалось целое число.";
            }
            if (v < 1 || v > 50) {
                return "Номер места должен быть от 1 до 50.";
            }
            // Проверяем занятость
            try {
                if (service.isSeatTaken(train, wagon, v, date)) {
                    return String.format(
                            "Место %d в вагоне %d на поезд %s (%s) уже занято.",
                            v, wagon, train, date);
                }
            } catch (DatabaseException e) {
                return "Не удалось проверить занятость места: " + e.getMessage();
            }
            return null;
        });
        return Integer.parseInt(line);
    }

    /**
     * Место при редактировании: разрешаем оставить «своё» место,
     * если бронь не меняла поезд/вагон/дату.
     */
    private int readFreeSeatForUpdate(String prompt, String train, int wagon,
                                      LocalDate date, Booking existing) {
        boolean sameContext =
                existing.getTrainNumber().equals(train)
                        && existing.getWagonNumber() == wagon
                        && existing.getDepartureDate().equals(date);

        String line = InputValidator.readValidated(scanner, prompt, s -> {
            int v;
            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return "Ожидалось целое число.";
            }
            if (v < 1 || v > 50) {
                return "Номер места должен быть от 1 до 50.";
            }
            // Если пользователь оставил своё же место — не проверяем занятость
            if (sameContext && v == existing.getSeatNumber()) {
                return null;
            }
            try {
                if (service.isSeatTaken(train, wagon, v, date)) {
                    return String.format(
                            "Место %d в вагоне %d на поезд %s (%s) уже занято.",
                            v, wagon, train, date);
                }
            } catch (DatabaseException e) {
                return "Не удалось проверить занятость места: " + e.getMessage();
            }
            return null;
        });
        return Integer.parseInt(line);
    }
}