package ru.mirea.railway.ui;

import static ru.mirea.railway.util.Ansi.*;
import static ru.mirea.railway.util.Gradient.*;

import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.util.InputValidator;

import java.util.List;

/**
 * Печать таблиц в консоль с выравниванием колонок.
 */
public final class TablePrinter {

    private TablePrinter() {}

    private static final int[] PINK = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    public static void printPassengers(List<Passenger> passengers) {
        if (passengers.isEmpty()) {
            System.out.println(between("(список пуст)", PINK, VIOLET));
            return;
        }
        String fmt = "| %-4s | %-30s | %-14s | %-25s | %-16s | %-10s |";
        String sep = "+------+--------------------------------+----------------+---------------------------+------------------+------------+";

        System.out.println(between(sep, PINK, VIOLET));
        System.out.println(between(String.format(fmt, "ID", "ФИО", "Паспорт", "Email", "Телефон", "Дата рожд."), PINK, VIOLET));
        System.out.println(between(sep, PINK, VIOLET));
        for (Passenger p : passengers) {
            System.out.println(between(String.format(fmt,
                    p.getId(),
                    truncate(p.getFullName(), 30),
                    truncate(p.getPassportNumber(), 14),
                    truncate(p.getEmail(), 25),
                    truncate(p.getPhone(), 16),
                    InputValidator.formatDate(p.getBirthDate())), PINK, VIOLET));
        }
        System.out.println(between(sep, PINK, VIOLET));
        System.out.println(between("Всего: " + passengers.size(), PINK, VIOLET));
    }

    public static void printBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println(between("(список пуст)", PINK, VIOLET));
            return;
        }
        String fmt = "| %-4s | %-4s | %-6s | %-15s | %-15s | %-10s | %-5s | %-4s | %-9s | %-12s |";
        String sep = "+------+------+--------+-----------------+-----------------+------------+-------+------+-----------+--------------+";

        System.out.println(between(sep, PINK, VIOLET));
        System.out.println(between(String.format(fmt, "ID", "Пас.", "Поезд", "Откуда", "Куда",
                "Дата", "Ваг.", "Мст.", "Цена", "Статус"), PINK, VIOLET));
        System.out.println(between(sep, PINK, VIOLET));
        for (Booking b : bookings) {
            System.out.println(between(String.format(fmt,
                    b.getId(),
                    b.getPassengerId(),
                    b.getTrainNumber(),
                    truncate(b.getRouteFrom(), 15),
                    truncate(b.getRouteTo(), 15),
                    InputValidator.formatDate(b.getDepartureDate()),
                    b.getWagonNumber(),
                    b.getSeatNumber(),
                    b.getPrice(),
                    b.getStatus().name()), PINK, VIOLET));
        }
        System.out.println(between(sep, PINK, VIOLET));
        System.out.println(between("Всего: " + bookings.size(), PINK, VIOLET));
    }

    public static void printSingleBooking(Booking b) {
        System.out.println(between("──────────────────────────────────────────────", PINK, VIOLET));
        System.out.println(between("  ID:            " + b.getId(), PINK, VIOLET));
        System.out.println(between("  Пассажир ID:   " + b.getPassengerId(), PINK, VIOLET));
        System.out.println(between("  Поезд:         " + b.getTrainNumber(), PINK, VIOLET));
        System.out.println(between("  Маршрут:       " + b.getRouteFrom() + " → " + b.getRouteTo(), PINK, VIOLET));
        System.out.println(between("  Отправление:   " + InputValidator.formatDate(b.getDepartureDate())
                + " в " + InputValidator.formatTime(b.getDepartureTime()), PINK, VIOLET));
        System.out.println(between("  Вагон / место: " + b.getWagonNumber() + " / " + b.getSeatNumber(), PINK, VIOLET));
        System.out.println(between("  Цена:          " + b.getPrice() + " ₽", PINK, VIOLET));
        System.out.println(between("  Статус:        " + b.getStatus(), PINK, VIOLET));
        System.out.println(between("  Создано:       " + b.getCreatedAt(), PINK, VIOLET));
        System.out.println(between("──────────────────────────────────────────────", PINK, VIOLET));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}