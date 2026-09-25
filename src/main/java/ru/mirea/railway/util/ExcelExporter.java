package ru.mirea.railway.util;

import static ru.mirea.railway.util.Gradient.between;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.model.Booking;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт бронирований в Excel (.xlsx) через Apache POI.
 *
 * Формирует таблицу с заголовками, автошириной колонок
 * и стилизованной шапкой.
 */
public final class ExcelExporter {

    private static final int[] PINK   = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String[] HEADERS = {
            "ID", "Пассажир ID", "Поезд", "Откуда", "Куда",
            "Дата отправления", "Время", "Вагон", "Место",
            "Цена, ₽", "Статус", "Создано"
    };

    private ExcelExporter() {}

    /**
     * Экспортирует список броней в .xlsx.
     *
     * @param bookings список броней
     * @param filePath путь к файлу (например, "export/bookings.xlsx")
     * @return количество выгруженных строк
     */
    public static int exportBookings(List<Booking> bookings, String filePath) {
        if (bookings == null) {
            throw new DatabaseException(between(
                    "Список броней для экспорта не может быть null", PINK, VIOLET));
        }

        Path path = Path.of(filePath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DatabaseException(between(
                    "Не удалось создать директорию для экспорта: " + e.getMessage(), PINK, VIOLET), null);
        }

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Bookings");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle   = createDateStyle(workbook);
            CellStyle moneyStyle  = createMoneyStyle(workbook);

            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, bookings, dateStyle, moneyStyle);

            // Автоширина под содержимое
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 512, 60 * 256));
            }

            workbook.write(out);
            return bookings.size();

        } catch (IOException e) {
            throw new DatabaseException(between(
                    "Ошибка экспорта в Excel: " + e.getMessage(), PINK, VIOLET), null);
        }
    }

    // =========================================================
    //  Внутренние методы
    // =========================================================

    private static void createHeaderRow(Sheet sheet, CellStyle style) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
        header.setHeightInPoints(20);
    }

    private static void fillDataRows(Sheet sheet,
                                     List<Booking> bookings,
                                     CellStyle dateStyle,
                                     CellStyle moneyStyle) {
        int rowIdx = 1;
        for (Booking b : bookings) {
            Row row = sheet.createRow(rowIdx++);

            setIntCell(row, 0, b.getId());
            setIntCell(row, 1, b.getPassengerId());
            setStringCell(row, 2, b.getTrainNumber());
            setStringCell(row, 3, b.getRouteFrom());
            setStringCell(row, 4, b.getRouteTo());

            // Дата отдельно
            Cell dateCell = row.createCell(5);
            dateCell.setCellValue(b.getDepartureDate() != null
                    ? b.getDepartureDate().format(DATE_FMT) : "—");

            // Время отдельно
            setStringCell(row, 6, b.getDepartureTime() != null
                    ? b.getDepartureTime().format(TIME_FMT) : "—");

            setIntCell(row, 7, b.getWagonNumber());
            setIntCell(row, 8, b.getSeatNumber());

            // Цена — числом, чтобы в Excel можно было суммировать
            Cell priceCell = row.createCell(9);
            if (b.getPrice() != null) {
                priceCell.setCellValue(b.getPrice().doubleValue());
                priceCell.setCellStyle(moneyStyle);
            } else {
                priceCell.setCellValue("—");
            }

            setStringCell(row, 10, b.getStatus() != null
                    ? b.getStatus().name() + " (" + b.getStatus().getDisplayName() + ")"
                    : "—");

            setStringCell(row, 11, b.getCreatedAt() != null
                    ? b.getCreatedAt().format(DATETIME_FMT) : "—");
        }
    }

    private static void setStringCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
    }

    private static void setIntCell(Row row, int col, long value) {
        row.createCell(col).setCellValue(value);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setBorders(style);
        return style;
    }

    private static CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static CellStyle createMoneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 \"₽\""));
        style.setAlignment(HorizontalAlignment.RIGHT);
        setBorders(style);
        return style;
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}