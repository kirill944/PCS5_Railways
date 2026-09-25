package ru.mirea.railway.util;

import static ru.mirea.railway.util.Ansi.*;
import static ru.mirea.railway.util.Gradient.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.function.Function;

public final class InputValidator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final int[] PINK = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    private InputValidator() {}

    /**
     * Печатает приглашение к вводу.
     * Если строка уже содержит ANSI-коды (окрашена вызывающим) — оставляем как есть,
     * иначе красим градиентом PINK→VIOLET.
     */
    private static void prompt(String message) {
        String out = message.contains("\u001B[")
                ? message
                : between(message, PINK, VIOLET);
        System.out.print(out);
        System.out.flush();
    }

    // =========================================================
    //  Универсальный ввод с немедленной валидацией
    // =========================================================

    /** Читает строку, пока валидатор не вернёт null (нет ошибки). */
    public static String readValidated(Scanner scanner, String prompt,
                                       Function<String, String> validator) {
        while (true) {
            prompt(prompt);
            String line = scanner.nextLine().trim();
            String error = validator.apply(line);
            if (error == null) {
                return line;
            }
            System.out.println(color("⚠ " + error + " Введите ещё раз:", YELLOW));
        }
    }

    /** Читает строку, соответствующую регулярному выражению. */
    public static String readRegex(Scanner scanner, String prompt,
                                   String regex, String errorMessage) {
        return readValidated(scanner, prompt, line -> {
            if (line.isEmpty()) {
                return "Поле не может быть пустым.";
            }
            return line.matches(regex) ? null : errorMessage;
        });
    }

    // =========================================================
    //  Базовые методы
    // =========================================================

    public static Integer readInt(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println(color(
                    "⚠ Ошибка: ожидалось целое число, получено: '" + line + "'", YELLOW));
            return null;
        }
    }

    public static int readIntRequired(Scanner scanner, String prompt) {
        while (true) {
            Integer v = readInt(scanner, prompt);
            if (v != null) {
                return v;
            }
        }
    }

    public static Long readLong(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            System.out.println(color(
                    "⚠ Ошибка: ожидалось целое число, получено: '" + line + "'", YELLOW));
            return null;
        }
    }

    public static long readLongRequired(Scanner scanner, String prompt) {
        while (true) {
            Long v = readLong(scanner, prompt);
            if (v != null) {
                return v;
            }
        }
    }

    public static String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            prompt(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println(color("⚠ Ошибка: строка не может быть пустой", YELLOW));
        }
    }

    public static String readString(Scanner scanner, String prompt) {
        prompt(prompt);
        return scanner.nextLine().trim();
    }

    public static LocalDate readDate(Scanner scanner, String prompt) {
        prompt(prompt + " (формат dd.mm.yyyy): ");
        String line = scanner.nextLine().trim();
        try {
            return LocalDate.parse(line, DATE_FMT);
        } catch (DateTimeParseException e) {
            System.out.println(color(
                    "⚠ Ошибка: некорректная дата '" + line
                            + "'. Ожидается dd.mm.yyyy", YELLOW));
            return null;
        }
    }

    public static LocalDate readDateRequired(Scanner scanner, String prompt) {
        while (true) {
            LocalDate v = readDate(scanner, prompt);
            if (v != null) {
                return v;
            }
        }
    }

    public static LocalTime readTime(Scanner scanner, String prompt) {
        prompt(prompt + " (формат HH:mm): ");
        String line = scanner.nextLine().trim();
        try {
            return LocalTime.parse(line, TIME_FMT);
        } catch (DateTimeParseException e) {
            System.out.println(color(
                    "⚠ Ошибка: некорректное время '" + line
                            + "'. Ожидается HH:mm", YELLOW));
            return null;
        }
    }

    public static LocalTime readTimeRequired(Scanner scanner, String prompt) {
        while (true) {
            LocalTime v = readTime(scanner, prompt);
            if (v != null) {
                return v;
            }
        }
    }

    public static BigDecimal readBigDecimal(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim().replace(',', '.');
        try {
            return new BigDecimal(line);
        } catch (NumberFormatException e) {
            System.out.println(color(
                    "⚠ Ошибка: ожидалось число, получено: '" + line + "'", YELLOW));
            return null;
        }
    }

    public static BigDecimal readBigDecimalRequired(Scanner scanner, String prompt) {
        while (true) {
            BigDecimal v = readBigDecimal(scanner, prompt);
            if (v != null) {
                return v;
            }
        }
    }

    public static boolean confirm(Scanner scanner, String prompt) {
        while (true) {
            prompt(prompt + " (y/n): ");
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes") || line.equals("д")) {
                return true;
            }
            if (line.equals("n") || line.equals("no") || line.equals("н")) {
                return false;
            }
            System.out.println(color("⚠ Введите 'y' или 'n'", YELLOW));
        }
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FMT);
    }
}