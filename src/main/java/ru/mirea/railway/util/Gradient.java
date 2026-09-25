package ru.mirea.railway.util;

public final class Gradient {

    private Gradient() {
    }

    private static final String RESET = "\u001B[0m";

    private static String rgb(int r, int g, int b) {
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }

    /**
     * Линейный градиент между двумя цветами.
     */
    public static String between(String text, int[] from, int[] to) {
        if (text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0 : (double) i / (n - 1);
            int r = (int) Math.round(from[0] + t * (to[0] - from[0]));
            int g = (int) Math.round(from[1] + t * (to[1] - from[1]));
            int b = (int) Math.round(from[2] + t * (to[2] - from[2]));
            sb.append(rgb(r, g, b)).append(text.charAt(i));
        }
        return sb.append(RESET).toString();
    }

    /**
     * Градиент через произвольное число цветов (по сегментам).
     */
    public static String multi(String text, int[]... stops) {
        if (text.isEmpty() || stops.length == 0) return text;
        if (stops.length == 1) return between(text, stops[0], stops[0]);
        StringBuilder sb = new StringBuilder();
        int n = text.length();
        int segments = stops.length - 1;
        for (int i = 0; i < n; i++) {
            double pos = n == 1 ? 0 : (double) i / (n - 1);   // 0..1
            double seg = pos * segments;                       // в какой сегмент попали
            int idx = Math.min((int) seg, segments - 1);
            double t = seg - idx;
            int[] a = stops[idx], b = stops[idx + 1];
            int r = (int) Math.round(a[0] + t * (b[0] - a[0]));
            int g = (int) Math.round(a[1] + t * (b[1] - a[1]));
            int c = (int) Math.round(a[2] + t * (b[2] - a[2]));
            sb.append(rgb(r, g, c)).append(text.charAt(i));
        }
        return sb.append(RESET).toString();
    }

    /**
     * Радуга: HSV-вращение по позиции символа.
     */
    public static String rainbow(String text) {
        return rainbow(text, 0.0);
    }

    public static String rainbow(String text, double hueShift) {
        StringBuilder sb = new StringBuilder();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            double hue = (hueShift + (double) i / Math.max(1, n)) % 1.0; // 0..1
            int[] rgb = hsvToRgb(hue, 1.0, 1.0);
            sb.append(rgb(rgb[0], rgb[1], rgb[2])).append(text.charAt(i));
        }
        return sb.append(RESET).toString();
    }

    /**
     * HSV → RGB, все значения 0..1, выход 0..255.
     */
    private static int[] hsvToRgb(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = v - c;
        double r, g, b;
        int seg = (int) (h * 6);
        switch (seg) {
            case 0 -> {
                r = c;
                g = x;
                b = 0;
            }
            case 1 -> {
                r = x;
                g = c;
                b = 0;
            }
            case 2 -> {
                r = 0;
                g = c;
                b = x;
            }
            case 3 -> {
                r = 0;
                g = x;
                b = c;
            }
            case 4 -> {
                r = x;
                g = 0;
                b = c;
            }
            default -> {
                r = c;
                g = 0;
                b = x;
            }
        }
        return new int[]{
                (int) Math.round((r + m) * 255),
                (int) Math.round((g + m) * 255),
                (int) Math.round((b + m) * 255)
        };
    }
}
