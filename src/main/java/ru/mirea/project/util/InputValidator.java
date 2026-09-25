package ru.mirea.project.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class InputValidator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private InputValidator() {}

    // =========================================================
    //  Вспомогательный метод — печатает приглашение и сбрасывает буфер
    // =========================================================
    private static void prompt(String message) {
        System.out.print(message);
        System.out.flush();   // ← ключевая строка
    }

    // =========================================================
    //  Чтение целых чисел
    // =========================================================

    public static Integer readInt(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'");
            return null;
        }
    }

    public static int readIntRequired(Scanner scanner, String prompt) {
        while (true) {
            Integer value = readInt(scanner, prompt);
            if (value != null) {
                return value;
            }
        }
    }

    public static Long readLong(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            System.out.println("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'");
            return null;
        }
    }

    public static long readLongRequired(Scanner scanner, String prompt) {
        while (true) {
            Long value = readLong(scanner, prompt);
            if (value != null) {
                return value;
            }
        }
    }

    // =========================================================
    //  Чтение строк
    // =========================================================

    public static String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            prompt(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("⚠ Ошибка: строка не может быть пустой");
        }
    }

    public static String readString(Scanner scanner, String prompt) {
        prompt(prompt);
        return scanner.nextLine().trim();
    }

    // =========================================================
    //  Чтение даты и времени
    // =========================================================

    public static LocalDate readDate(Scanner scanner, String prompt) {
        prompt(prompt + " (формат dd.MM.yyyy): ");
        String line = scanner.nextLine().trim();
        try {
            return LocalDate.parse(line, DATE_FMT);
        } catch (DateTimeParseException e) {
            System.out.println("⚠ Ошибка: некорректная дата '" + line
                    + "'. Ожидается dd.MM.yyyy");
            return null;
        }
    }

    public static LocalDate readDateRequired(Scanner scanner, String prompt) {
        while (true) {
            LocalDate value = readDate(scanner, prompt);
            if (value != null) {
                return value;
            }
        }
    }

    public static LocalTime readTime(Scanner scanner, String prompt) {
        prompt(prompt + " (формат HH:mm): ");
        String line = scanner.nextLine().trim();
        try {
            return LocalTime.parse(line, TIME_FMT);
        } catch (DateTimeParseException e) {
            System.out.println("⚠ Ошибка: некорректное время '" + line
                    + "'. Ожидается HH:mm");
            return null;
        }
    }

    public static LocalTime readTimeRequired(Scanner scanner, String prompt) {
        while (true) {
            LocalTime value = readTime(scanner, prompt);
            if (value != null) {
                return value;
            }
        }
    }

    // =========================================================
    //  Чтение денежных сумм
    // =========================================================

    public static BigDecimal readBigDecimal(Scanner scanner, String prompt) {
        prompt(prompt);
        String line = scanner.nextLine().trim().replace(',', '.');
        try {
            return new BigDecimal(line);
        } catch (NumberFormatException e) {
            System.out.println("⚠ Ошибка: ожидалось число, получено: '" + line + "'");
            return null;
        }
    }

    public static BigDecimal readBigDecimalRequired(Scanner scanner, String prompt) {
        while (true) {
            BigDecimal value = readBigDecimal(scanner, prompt);
            if (value != null) {
                return value;
            }
        }
    }

    // =========================================================
    //  Подтверждение действия (y/n)
    // =========================================================

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
            System.out.println("⚠ Введите 'y' или 'n'");
        }
    }

    // =========================================================
    //  Форматирование
    // =========================================================

    public static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FMT);
    }
}