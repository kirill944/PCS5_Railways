package ru.mirea.railway.util;

import static ru.mirea.railway.util.Ansi.*;

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

    public static final String TRAIN_REGEX = "^[0-9]{3}[АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЭЮЯ]$";

    public static final String TRAIN_ERROR =
            "Неверный формат номера поезда. Ожидается ровно 3 цифры и одна "
                    + "ЗАГЛАВНАЯ русская буква, кроме 'Ь', 'Ъ', 'Ё', 'Ы'. "
                    + "Например: 123А, 045Б.";

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
            System.out.println(color("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'", YELLOW));
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
            System.out.println(color("⚠ Ошибка: ожидалось целое число, получено: '" + line + "'", YELLOW));
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
        prompt(prompt + " (формат dd.MM.yyyy): ");
        String line = scanner.nextLine().trim();
        try {
            return LocalDate.parse(line, DATE_FMT);
        } catch (DateTimeParseException e) {
            System.out.println(color("⚠ Ошибка: некорректная дата '" + line
                    + "'. Ожидается dd.MM.yyyy", YELLOW));
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
            System.out.println(color("⚠ Ошибка: некорректное время '" + line
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
            System.out.println(color("⚠ Ошибка: ожидалось число, получено: '" + line + "'", YELLOW));
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

    // =========================================================
    //  Подтверждение y/n — НЕ выходит из программы при неверном вводе
    // =========================================================

    /**
     * Читает подтверждение пользователя. Возвращает true для 'y'/'yes'/'д',
     * false для 'n'/'no'/'н'. Любой другой ввод — повторяет вопрос.
     * Никогда не выходит из программы и не бросает исключений.
     */
    public static boolean confirm(Scanner scanner, String prompt) {
        while (true) {
            prompt(prompt + " (y/n): ");
            String line = scanner.nextLine().trim().toLowerCase();

            if (line.equals("y") || line.equals("yes") || line.equals("д")
                    || line.equals("да")) {
                return true;
            }
            if (line.equals("n") || line.equals("no") || line.equals("н")
                    || line.equals("нет")) {
                return false;
            }

            System.out.println(color("⚠ Неверный ввод: '" + line
                    + "'. Допустимы только 'y' (да) или 'n' (нет). Попробуйте снова:", YELLOW));
        }
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FMT);
    }

    // =========================================================
    //  Специфичные поля с бизнес-ограничениями
    // =========================================================

    /**
     * Дата отправления:
     *   - формат dd.MM.yyyy;
     *   - не в прошлом;
     *   - не дальше 1 года вперёд.
     */
    public static LocalDate readDepartureDate(Scanner scanner, String prompt) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusYears(1);

        String line = readValidated(scanner, prompt + " (dd.MM.yyyy): ", s -> {
            try {
                LocalDate d = LocalDate.parse(s, DATE_FMT);
                if (d.isBefore(today)) {
                    return "Дата отправления не может быть в прошлом.";
                }
                if (d.isAfter(maxDate)) {
                    return "Дата отправления не может быть позже "
                            + maxDate.format(DATE_FMT) + " (не более 1 года вперёд).";
                }
                return null;
            } catch (Exception e) {
                return "Неверный формат даты. Ожидается dd.MM.yyyy.";
            }
        });
        return LocalDate.parse(line, DATE_FMT);
    }

    /** Номер вагона: целое 1–20. */
    public static int readWagonNumber(Scanner scanner, String prompt) {
        String line = readValidated(scanner, prompt, s -> {
            try {
                int v = Integer.parseInt(s);
                if (v < 1 || v > 20) {
                    return "Номер вагона должен быть от 1 до 20.";
                }
                return null;
            } catch (NumberFormatException e) {
                return "Ожидалось целое число.";
            }
        });
        return Integer.parseInt(line);
    }

    /** Номер места: целое 1–50. */
    public static int readSeatNumber(Scanner scanner, String prompt) {
        String line = readValidated(scanner, prompt, s -> {
            try {
                int v = Integer.parseInt(s);
                if (v < 1 || v > 50) {
                    return "Номер места должен быть от 1 до 50.";
                }
                return null;
            } catch (NumberFormatException e) {
                return "Ожидалось целое число.";
            }
        });
        return Integer.parseInt(line);
    }

    /** Цена билета: от 1000 до 100000 рублей. */
    public static BigDecimal readPrice(Scanner scanner, String prompt) {
        String line = readValidated(scanner, prompt, s -> {
            try {
                BigDecimal v = new BigDecimal(s.replace(',', '.'));
                if (v.compareTo(new BigDecimal("1000")) < 0) {
                    return "Цена не может быть меньше 1000 ₽.";
                }
                if (v.compareTo(new BigDecimal("100000")) > 0) {
                    return "Цена не может быть больше 100 000 ₽.";
                }
                return null;
            } catch (NumberFormatException e) {
                return "Ожидалось число.";
            }
        });
        return new BigDecimal(line.replace(',', '.'));
    }
}