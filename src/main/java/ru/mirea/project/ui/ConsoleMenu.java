package ru.mirea.project.ui;

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
    private final BookingService bookingService     = new BookingService();
    private final StatisticsService statisticsService = new StatisticsService();

    public void run() {
        printBanner();

        if (!DatabaseManager.getInstance().isAvailable()) {
            System.out.println("✖ База данных недоступна. Проверьте db.properties и PostgreSQL.");
            return;
        }
        System.out.println("✔ Подключение к БД установлено\n");

        while (true) {
            printMainMenu();
            Integer choice = InputValidator.readInt(scanner, "Выберите действие: ");
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
                        System.out.println("До свидания!");
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

    private void printBanner() {
        System.out.println("""
                ========================================
                   СИСТЕМА БРОНИРОВАНИЯ Ж/Д БИЛЕТОВ
                ========================================
                """);
    }

    private void printMainMenu() {
        System.out.println("""
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
                """);
    }

    // =========================================================
    //  ПОИСК
    // =========================================================

    private void searchMenu() {
        System.out.println("""
                ──── ПОИСК ────
                1. По номеру поезда
                2. По маршруту
                3. По ФИО пассажира
                4. По номеру паспорта
                0. Назад
                """);
        Integer choice = InputValidator.readInt(scanner, "Выберите: ");
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> bookingService.searchByTrainNumber(
                    InputValidator.readNonEmptyString(scanner, "Номер поезда: "));
            case 2 -> {
                String from = InputValidator.readString(scanner, "Откуда (Enter — пропустить): ");
                String to   = InputValidator.readString(scanner, "Куда (Enter — пропустить): ");
                yield bookingService.searchByRoute(from, to);
            }
            case 3 -> bookingService.searchByPassengerName(
                    InputValidator.readNonEmptyString(scanner, "Фрагмент ФИО: "));
            case 4 -> bookingService.searchByPassengerPassport(
                    InputValidator.readNonEmptyString(scanner, "Номер паспорта: "));
            default -> { System.out.println("Отменено"); yield List.of(); }
        };
        TablePrinter.printBookings(result);
    }

    // =========================================================
    //  ФИЛЬТРАЦИЯ
    // =========================================================

    private void filterMenu() {
        System.out.println("""
                ──── ФИЛЬТРАЦИЯ ────
                1. По статусу
                2. По пассажиру
                3. По диапазону дат
                0. Назад
                """);
        Integer choice = InputValidator.readInt(scanner, "Выберите: ");
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> {
                System.out.println("Статусы: CREATED, CONFIRMED, PAID, COMPLETED, CANCELLED");
                String s = InputValidator.readNonEmptyString(scanner, "Статус: ");
                yield bookingService.filterByStatus(BookingStatus.fromString(s));
            }
            case 2 -> {
                long pid = InputValidator.readLongRequired(scanner, "ID пассажира: ");
                yield bookingService.filterByPassenger(pid);
            }
            case 3 -> {
                LocalDate from = InputValidator.readDateRequired(scanner, "Дата от");
                LocalDate to   = InputValidator.readDateRequired(scanner, "Дата до");
                yield bookingService.filterByDateRange(from, to);
            }
            default -> { System.out.println("Отменено"); yield List.of(); }
        };
        TablePrinter.printBookings(result);
    }

    // =========================================================
    //  СОРТИРОВКА
    // =========================================================

    private void sortMenu() {
        System.out.println("""
                ──── СОРТИРОВКА ────
                1. По дате отправления (возр.)
                2. По дате отправления (убыв.)
                3. По цене (возр.)
                4. По цене (убыв.)
                5. По номеру поезда
                6. По статусу
                0. Назад
                """);
        Integer choice = InputValidator.readInt(scanner, "Выберите: ");
        if (choice == null) return;

        List<Booking> result = switch (choice) {
            case 1 -> bookingService.sortByDepartureDate(true);
            case 2 -> bookingService.sortByDepartureDate(false);
            case 3 -> bookingService.sortByPrice(true);
            case 4 -> bookingService.sortByPrice(false);
            case 5 -> bookingService.sortByTrainNumber();
            case 6 -> bookingService.sortByStatus();
            default -> { System.out.println("Отменено"); yield List.of(); }
        };
        TablePrinter.printBookings(result);
    }

    // =========================================================
    //  СТАТИСТИКА
    // =========================================================

    private void showStatistics() {
        StatisticsService.Statistics stats = statisticsService.collect();
        System.out.println("""
                ╔════════════════════ СТАТИСТИКА ════════════════════╗""");
        System.out.printf("║ Всего пассажиров:          %-23d║%n", stats.totalPassengers());
        System.out.printf("║ Всего бронирований:        %-23d║%n", stats.totalBookings());
        System.out.printf("║ Активных (CREATED/CONF/PAID): %-20d║%n", stats.activeBookings());
        System.out.printf("║ Завершённых (COMPLETED):   %-23d║%n", stats.completedBookings());
        System.out.printf("║ Отменённых (CANCELLED):    %-23d║%n", stats.cancelledBookings());
        System.out.printf("║ Средняя цена билета:       %-23s║%n", stats.averagePrice() + " ₽");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║ Брони по поездам:                                 ║");
        stats.bookingsByTrain().forEach((train, count) ->
                System.out.printf("║   %-10s  →  %-32d║%n", train, count));
        System.out.println("╚═══════════════════════════════════════════════════╝");
    }

    // =========================================================
    //  ЭКСПОРТ
    // =========================================================

    private void exportToExcel() {
        String path = "export/bookings.xlsx";
        int count = ExcelExporter.exportBookings(bookingService.findAll(), path);
        System.out.println("✔ Экспортировано " + count + " записей в " + path);
    }

    // =========================================================
    //  ВЫВОД ТАБЛИЦ БД
    // =========================================================

    private void showDatabaseTables() {
        List<Passenger> passengers = passengerService.findAll();
        List<Booking> bookings = bookingService.findAll();

        System.out.println("──── Таблица passengers ────");
        TablePrinter.printPassengers(passengers);
        System.out.println();
        System.out.println("──── Таблица bookings ────");
        TablePrinter.printBookings(bookings);
    }
}