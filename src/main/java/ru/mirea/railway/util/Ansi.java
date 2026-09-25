package ru.mirea.railway.util;

public final class Ansi {

    private Ansi() {}

    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";

    // ── базовые цвета (16) ──
    public static final String BLACK   = ESC + "30m";
    public static final String RED     = ESC + "31m";
    public static final String GREEN   = ESC + "32m";
    public static final String YELLOW  = ESC + "38;5;226m";
    public static final String BLUE    = ESC + "34m";
    public static final String MAGENTA = ESC + "35m";   // ← пурпурный
    public static final String CYAN    = ESC + "36m";
    public static final String WHITE   = ESC + "37m";

    // яркие (bright) — выглядят как «розовый» на многих терминалах
    public static final String BRIGHT_MAGENTA = ESC + "95m";
    public static final String BRIGHT_PINK    = ESC + "38;5;213m"; // 256-цветный, настоящий розовый
    public static final String HOT_PINK       = ESC + "38;2;255;105;180m"; // truecolor #FF69B4

    // стили
    public static final String BOLD      = ESC + "1m";
    public static final String UNDERLINE = ESC + "4m";

    /** Обернуть текст цветом и сбросить в конце. */
    public static String color(String text, String colorCode) {
        return colorCode + text + RESET;
    }

    public static String pink(String text)      { return HOT_PINK + text + RESET; }
    public static String brightPink(String text){ return BRIGHT_PINK + text + RESET; }
    public static String magenta(String text)   { return BRIGHT_MAGENTA + text + RESET; }
    public static String bold(String text)      { return BOLD + text + RESET; }
}


