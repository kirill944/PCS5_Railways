package ru.mirea.railway.ui;

import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.util.InputValidator;

import java.util.List;

/**
 * Печать таблиц в консоль с выравниванием колонок.
 */
public final class TablePrinter {

    private TablePrinter() {}

    public static void printPassengers(List<Passenger> passengers) {
        if (passengers.isEmpty()) {
            System.out.println("(список пуст)");
            return;
        }
        String fmt = "| %-4s | %-30s | %-14s | %-25s | %-16s | %-10s |%n";
        String sep = "+------+--------------------------------+----------------+---------------------------+------------------+------------+";

        System.out.println(sep);
        System.out.printf(fmt, "ID", "ФИО", "Паспорт", "Email", "Телефон", "Дата рожд.");
        System.out.println(sep);
        for (Passenger p : passengers) {
            System.out.printf(fmt,
                    p.getId(),
                    truncate(p.getFullName(), 30),
                    truncate(p.getPassportNumber(), 14),
                    truncate(p.getEmail(), 25),
                    truncate(p.getPhone(), 16),
                    InputValidator.formatDate(p.getBirthDate()));
        }
        System.out.println(sep);
        System.out.println("Всего: " + passengers.size());
    }

    public static void printBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println("(список пуст)");
            return;
        }
        String fmt = "| %-4s | %-4s | %-6s | %-15s | %-15s | %-10s | %-5s | %-4s | %-9s | %-12s |%n";
        String sep = "+------+------+--------+-----------------+-----------------+------------+-------+------+-----------+--------------+";

        System.out.println(sep);
        System.out.printf(fmt, "ID", "Пас.", "Поезд", "Откуда", "Куда",
                "Дата", "Ваг.", "Мст.", "Цена", "Статус");
        System.out.println(sep);
        for (Booking b : bookings) {
            System.out.printf(fmt,
                    b.getId(),
                    b.getPassengerId(),
                    b.getTrainNumber(),
                    truncate(b.getRouteFrom(), 15),
                    truncate(b.getRouteTo(), 15),
                    InputValidator.formatDate(b.getDepartureDate()),
                    b.getWagonNumber(),
                    b.getSeatNumber(),
                    b.getPrice(),
                    b.getStatus().name());
        }
        System.out.println(sep);
        System.out.println("Всего: " + bookings.size());
    }

    public static void printSingleBooking(Booking b) {
        System.out.println("──────────────────────────────────────────────");
        System.out.println("  ID:            " + b.getId());
        System.out.println("  Пассажир ID:   " + b.getPassengerId());
        System.out.println("  Поезд:         " + b.getTrainNumber());
        System.out.println("  Маршрут:       " + b.getRouteFrom() + " → " + b.getRouteTo());
        System.out.println("  Отправление:   " + InputValidator.formatDate(b.getDepartureDate())
                + " в " + InputValidator.formatTime(b.getDepartureTime()));
        System.out.println("  Вагон / место: " + b.getWagonNumber() + " / " + b.getSeatNumber());
        System.out.println("  Цена:          " + b.getPrice() + " ₽");
        System.out.println("  Статус:        " + b.getStatus());
        System.out.println("  Создано:       " + b.getCreatedAt());
        System.out.println("──────────────────────────────────────────────");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}