package ru.mirea.railway.util;

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

    private InputValidator() {}

    private static void prompt(String message) {
        System.out.print(message);
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
            System.out.println("⚠ " + error + " Введите ещё раз:");
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
            System.out.println("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'");
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
            System.out.println("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'");
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
            System.out.println("⚠ Ошибка: строка не может быть пустой");
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
            System.out.println("⚠ Ошибка: некорректная дата '" + line
                    + "'. Ожидается dd.mm.yyyy");
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
            System.out.println("⚠ Ошибка: некорректное время '" + line
                    + "'. Ожидается HH:mm");
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
            System.out.println("⚠ Ошибка: ожидалось число, получено: '" + line + "'");
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
            System.out.println("⚠ Введите 'y' или 'n'");
        }
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FMT);
    }
}