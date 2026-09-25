package ru.mirea.railway.ui;

import static ru.mirea.railway.util.Ansi.*;
import static ru.mirea.railway.util.Gradient.*;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.service.BookingService;
import ru.mirea.railway.service.PassengerService;
import ru.mirea.railway.service.StatisticsService;
import ru.mirea.railway.util.DatabaseManager;
import ru.mirea.railway.util.ExcelExporter;
import ru.mirea.railway.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Главное консольное меню системы бронирования Ж/Д билетов.
 */
public class ConsoleMenu {

    private final Scanner scanner = new Scanner(System.in);

    private final PassengerService passengerService = new PassengerService();
    private final BookingService bookingService = new BookingService();
    private final StatisticsService statisticsService = new StatisticsService();

    public void run() {
        printBanner();

        if (!DatabaseManager.getInstance().isAvailable()) {
            System.out.println(color("✖ База данных недоступна. Проверьте db.properties и PostgreSQL.", RED));
            return;
        }
        System.out.println(between("✔ Подключение к БД установлено\n", PINK, VIOLET));

        while (true) {
            printMainMenu();
            Integer choice = InputValidator.readInt(scanner, between("Выберите действие: ", PINK, VIOLET));
            if (choice == null) continue;

            try {
                switch (choice) {
                    case 1 -> new PassengerMenu(scanner, passengerService).show();
                    case 2 -> new BookingMenu(scanner, bookingService).show();
                    case 3 -> searchMenu();
                    case 4 -> filterMenu();
                    case 5 -> sortMenu();
                    case 6 -> showStatistics();
                    case 7 -> exportToExcel();
                    case 8 -> showDatabaseTables();
                    case 0 -> {
                        System.out.println(between("До свидания!", PINK, VIOLET));
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

    private static final int[] PINK = {255, 105, 180};   // #FF69B4
    private static final int[] DEEP_PINK = {199, 21, 133};   // #C71585
    private static final int[] VIOLET = {138, 43, 226};   // #8A2BE2
    private static final int[] CYAN = {0, 255, 255};

    private void printBanner() {

        System.out.println(between("""
                ========================================
                   СИСТЕМА БРОНИРОВАНИЯ Ж/Д БИЛЕТОВ
                ========================================
                """, PINK, VIOLET));
    }

    private void printMainMenu() {
        System.out.println(between("""
                ================ ГЛАВНОЕ МЕНЮ ================
                1. Пассажиры
                2. Бронирования
                3. Поиск
                4. Фильтрация
                5. Сортировка
                6. Статистика
                7. Экспорт данных в Excel
                8. Вывести таблицы базы данных
                0. Выход
                ==============================================
                """, PINK, VIOLET));
    }

    //  ПОИСК

    private void searchMenu() {
        System.out.println(between("""
                ──── ПОИСК ────
                1. По номеру поезда
                2. По маршруту
                3. По ФИО пассажира
                4. По номеру паспорта
                0. Назад
                """, PINK, VIOLET));
        Integer choice = InputValidator.readInt(scanner, between("Выберите: ", PINK, VIOLET));
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> bookingService.searchByTrainNumber(
                    InputValidator.readNonEmptyString(scanner, between("Номер поезда: ", PINK, VIOLET)));
            case 2 -> {
                String from = InputValidator.readString(scanner, between("Откуда (Enter — пропустить): ", PINK, VIOLET));
                String to = InputValidator.readString(scanner, between("Куда (Enter — пропустить): ", PINK, VIOLET));
                yield bookingService.searchByRoute(from, to);
            }
            case 3 -> bookingService.searchByPassengerName(
                    InputValidator.readNonEmptyString(scanner, between("Фрагмент ФИО: ", PINK, VIOLET)));
            case 4 -> bookingService.searchByPassengerPassport(
                    InputValidator.readNonEmptyString(scanner, between("Номер паспорта: ", PINK, VIOLET)));
            default -> {
                System.out.println(between("Отменено", PINK, VIOLET));
                yield List.of();
            }
        };
        TablePrinter.printBookings(result);
    }

    //  ФИЛЬТРАЦИЯ

    private void filterMenu() {
        System.out.println(between("""
                ──── ФИЛЬТРАЦИЯ ────
                1. По статусу
                2. По пассажиру
                3. По диапазону дат
                0. Назад
                """, PINK, VIOLET));
        Integer choice = InputValidator.readInt(scanner, between("Выберите: ", PINK, VIOLET));
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> {
                System.out.println(between("Статусы: CREATED, CONFIRMED, PAID, COMPLETED, CANCELLED", PINK, VIOLET));
                String s = InputValidator.readNonEmptyString(scanner, between("Статус: ", PINK, VIOLET));
                yield bookingService.filterByStatus(BookingStatus.fromString(s));
            }
            case 2 -> {
                long pid = InputValidator.readLongRequired(scanner, between("ID пассажира: ", PINK, VIOLET));
                yield bookingService.filterByPassenger(pid);
            }
            case 3 -> {
                LocalDate from = InputValidator.readDateRequired(scanner, between("Дата от", PINK, VIOLET));
                LocalDate to = InputValidator.readDateRequired(scanner, between("Дата до", PINK, VIOLET));
                yield bookingService.filterByDateRange(from, to);
            }
            default -> {
                System.out.println(between("Отменено", PINK, VIOLET));
                yield List.of();
            }
        };
        TablePrinter.printBookings(result);
    }

    //  СОРТИРОВКА

    private void sortMenu() {
        System.out.println(between("""
                ──── СОРТИРОВКА ────
                1. По дате отправления (возр.)
                2. По дате отправления (убыв.)
                3. По цене (возр.)
                4. По цене (убыв.)
                5. По номеру поезда
                6. По статусу
                0. Назад
                """, PINK, VIOLET));
        Integer choice = InputValidator.readInt(scanner, between("Выберите: ", PINK, VIOLET));
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> bookingService.sortByDepartureDate(true);
            case 2 -> bookingService.sortByDepartureDate(false);
            case 3 -> bookingService.sortByPrice(true);
            case 4 -> bookingService.sortByPrice(false);
            case 5 -> bookingService.sortByTrainNumber();
            case 6 -> bookingService.sortByStatus();
            default -> {
                System.out.println(between("Отменено", PINK, VIOLET));
                yield List.of();
            }
        };
        TablePrinter.printBookings(result);
    }

    //  СТАТИСТИКА

    private void showStatistics() {
        StatisticsService.Statistics stats = statisticsService.collect();
        System.out.println(between(
                "╔════════════════════ СТАТИСТИКА ════════════════════╗", PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Всего пассажиров:          %-23d║", stats.totalPassengers()), PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Всего бронирований:        %-23d║", stats.totalBookings()), PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Активных (CREATED/CONF/PAID): %-20d║", stats.activeBookings()), PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Завершённых (COMPLETED):   %-23d║", stats.completedBookings()), PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Отменённых (CANCELLED):    %-23d║", stats.cancelledBookings()), PINK, VIOLET));
        System.out.println(between(String.format(
                "║ Средняя цена билета:       %-23s║", stats.averagePrice() + " ₽"), PINK, VIOLET));
        System.out.println(between(
                "╠═══════════════════════════════════════════════════╣", PINK, VIOLET));
        System.out.println(between(
                "║ Брони по поездам:                                 ║", PINK, VIOLET));
        stats.bookingsByTrain().forEach((train, count) ->
                System.out.println(between(String.format(
                        "║   %-10s  →  %-32d║", train, count), PINK, VIOLET)));
        System.out.println(between(
                "╚═══════════════════════════════════════════════════╝", PINK, VIOLET));
    }

    //  ЭКСПОРТ

    private void exportToExcel() {
        String path = "export/bookings.xlsx";
        int count = ExcelExporter.exportBookings(bookingService.findAll(), path);
        System.out.println(between("✔ Экспортировано " + count + " записей в " + path, PINK, VIOLET));
    }

    //  ВЫВОД ТАБЛИЦ БД

    private void showDatabaseTables() {
        List<Passenger> passengers = passengerService.findAll();
        List<Booking> bookings = bookingService.findAll();

        System.out.println(between("──── Таблица passengers ────", PINK, VIOLET));
        TablePrinter.printPassengers(passengers);
        System.out.println();
        System.out.println(between("──── Таблица bookings ────", PINK, VIOLET));
        TablePrinter.printBookings(bookings);
    }
}