package ru.mirea.railway.fx.util;

import ru.mirea.railway.model.Booking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class CsvExporter {

    private static final String SEP = ";";

    private CsvExporter() {}

    public static void exportBookings(List<Booking> bookings,
                                      Map<Long, String> passengerNames,
                                      Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM — корректная кириллица в Excel
            w.write("ID;Пассажир;Поезд;Откуда;Куда;Дата;Время;Вагон;Место;Цена;Статус");
            w.newLine();

            for (Booking b : bookings) {
                w.write(String.join(SEP,
                        str(b.getId()),
                        esc(passengerNames.getOrDefault(b.getPassengerId(),
                                "ID=" + b.getPassengerId())),
                        esc(b.getTrainNumber()),
                        esc(b.getRouteFrom()),
                        esc(b.getRouteTo()),
                        str(b.getDepartureDate()),
                        str(b.getDepartureTime()),
                        str(b.getWagonNumber()),
                        str(b.getSeatNumber()),
                        b.getPrice() == null ? "" : b.getPrice().toPlainString(),
                        b.getStatus() == null ? "" : b.getStatus().getDisplayName()));
                w.newLine();
            }
        }
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(SEP) || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}