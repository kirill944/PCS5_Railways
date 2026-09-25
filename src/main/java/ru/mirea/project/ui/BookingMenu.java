package ru.mirea.project.ui;

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
import java.util.List;
import java.util.Scanner;

/**
 * Подменю работы с бронированиями.
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
            if (choice == null) continue;

            try {
                switch (choice) {
                    case 1 -> createBooking();
                    case 2 -> listAll();
                    case 3 -> findById();
                    case 4 -> updateBooking();
                    case 5 -> deleteBooking();
                    case 6 -> changeStatus();
                    case 0 -> { return; }
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
        String train     = InputValidator.readNonEmptyString(scanner, "Номер поезда: ");
        String from      = InputValidator.readNonEmptyString(scanner, "Станция отправления: ");
        String to        = InputValidator.readNonEmptyString(scanner, "Станция назначения: ");
        LocalDate date   = InputValidator.readDateRequired(scanner, "Дата отправления");
        LocalTime time   = InputValidator.readTimeRequired(scanner, "Время отправления");
        int wagon        = InputValidator.readIntRequired(scanner, "Номер вагона: ");
        int seat         = InputValidator.readIntRequired(scanner, "Номер места: ");
        BigDecimal price = InputValidator.readBigDecimalRequired(scanner, "Цена билета: ");

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
        if (id == null) return;
        TablePrinter.printSingleBooking(service.findById(id));
    }

    private void updateBooking() {
        Long id = InputValidator.readLong(scanner, "ID брони для редактирования: ");
        if (id == null) return;
        Booking existing = service.findById(id);

        System.out.println("Текущие данные:");
        TablePrinter.printSingleBooking(existing);

        long passengerId = InputValidator.readLongRequired(scanner, "ID пассажира: ");
        String train     = InputValidator.readNonEmptyString(scanner, "Номер поезда: ");
        String from      = InputValidator.readNonEmptyString(scanner, "Станция отправления: ");
        String to        = InputValidator.readNonEmptyString(scanner, "Станция назначения: ");
        LocalDate date   = InputValidator.readDateRequired(scanner, "Дата отправления");
        LocalTime time   = InputValidator.readTimeRequired(scanner, "Время отправления");
        int wagon        = InputValidator.readIntRequired(scanner, "Номер вагона: ");
        int seat         = InputValidator.readIntRequired(scanner, "Номер места: ");
        BigDecimal price = InputValidator.readBigDecimalRequired(scanner, "Цена: ");

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
        if (id == null) return;
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
        if (id == null) return;
        Booking b = service.findById(id);
        System.out.println("Текущий статус: " + b.getStatus());
        System.out.println("Доступные статусы:");
        for (BookingStatus s : BookingStatus.values()) {
            System.out.println("  - " + s.name() + " (" + s.getDisplayName() + ")");
        }
        String input = InputValidator.readNonEmptyString(scanner, "Новый статус: ");
        BookingStatus newStatus = BookingStatus.fromString(input);

        service.changeStatus(id, newStatus);
        System.out.println("✔ Статус изменён на " + newStatus);
    }
}