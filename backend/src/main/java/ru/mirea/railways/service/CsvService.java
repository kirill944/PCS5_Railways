package ru.mirea.railways.service;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.Booking;
import ru.mirea.railways.domain.Passenger;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.filter.BookingFilter;
import ru.mirea.railways.repository.BookingRepository;
import ru.mirea.railways.repository.PassengerRepository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class CsvService {
    private final BookingRepository bookings;
    private final PassengerRepository passengers;
    private final BookingService bookingService;
    private final Map<String, String> previewCache = new LinkedHashMap<>();

    /** Колонки, без которых разбор CSV бессмысленен (молча давал «0 строк»). */
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "trainNumber", "routeFrom", "routeTo", "departureDate",
            "departureTime", "wagonNumber", "seatNumber", "price", "passengerEmail");

    public CsvService(BookingRepository bookings, PassengerRepository passengers, BookingService bookingService) {
        this.bookings = bookings; this.passengers = passengers; this.bookingService = bookingService;
    }

    public String exportCsv(BookingFilter filter, int max) {
        var page = bookingService.search(filter, 0, Math.min(max, 1000), "createdAt", "desc");
        StringBuilder sb = new StringBuilder();
        sb.append("externalCode,trainNumber,routeFrom,routeTo,departureDate,departureTime,")
                .append("wagonNumber,seatNumber,price,status,paid,passengerEmail\n");
        for (BookingResponse b : page.content()) {
            sb.append(String.join(",",
                    b.externalCode(), b.trainNumber(), b.routeFrom(), b.routeTo(),
                    b.departureDate().toString(), b.departureTime().toString(),
                    String.valueOf(b.wagonNumber()), String.valueOf(b.seatNumber()),
                    b.price().toPlainString(), b.status(), String.valueOf(b.paid()), b.passengerName()
            )).append("\n");
        }
        return sb.toString();
    }

    public ImportPreviewResponse preview(ImportPreviewRequest r) {
        List<String> errors = new ArrayList<>();
        int valid = 0, invalid = 0;
        try (CSVParser parser = CSVParser.parse(r.csvContent(), CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines())) {

            requireHeaders(parser, r.mode());

            for (CSVRecord rec : parser) {
                try {
                    validateRecord(rec, r.mode());
                    valid++;
                } catch (Exception e) {
                    invalid++;
                    errors.add("Строка " + rec.getRecordNumber() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Не удалось разобрать CSV: " + e.getMessage());
        }
        String token = UUID.randomUUID().toString();
        previewCache.put(token, r.csvContent() + "|" + r.mode());
        return new ImportPreviewResponse(valid + invalid, valid, invalid, errors, token);
    }

    @Transactional
    public ImportPreviewResponse apply(ImportApplyRequest req) {
        String payload = previewCache.remove(req.token());
        if (payload == null) throw new BusinessException("Токен импорта не найден или устарел");
        String[] parts = payload.split("\\|", 2);
        String csv = parts[0];
        String mode = parts[1];
        int created = 0, updated = 0, invalid = 0;
        List<String> errors = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(csv, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreEmptyLines())) {
            requireHeaders(parser, mode);
            for (CSVRecord rec : parser) {
                try {
                    if ("UPDATE".equalsIgnoreCase(mode)) {
                        // Ищем по коду из CSV. Раньше здесь был bookings.findAll() в цикле
                        // (полный скан на каждую строку) и при совпадении делался только
                        // updated++ — ни одно поле не записывалось, UPDATE не работал.
                        String code = rec.get("externalCode");
                        Optional<Booking> existing = bookings.findByExternalCodeIgnoreCase(code);
                        if (existing.isPresent()) {
                            applyRecordTo(existing.get(), rec);
                            bookings.save(existing.get());
                            updated++;
                            continue;
                        }
                    }
                    applyRecord(rec);
                    created++;
                } catch (Exception e) {
                    invalid++;
                    errors.add("Строка " + rec.getRecordNumber() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Ошибка применения CSV: " + e.getMessage());
        }
        return new ImportPreviewResponse(created + updated + invalid, created + updated, invalid, errors, null);
    }

    /**
     * Проверяет наличие обязательных колонок. Без этой проверки CSV вида "a,b,c"
     * считался корректным и давал 200 с нулём строк вместо понятной ошибки.
     */
    private void requireHeaders(CSVParser parser, String mode) {
        Map<String, Integer> headers = parser.getHeaderMap();
        if (headers == null || headers.isEmpty()) {
            throw new BusinessException("CSV без заголовка: первая строка должна содержать "
                    + "названия колонок, например: " + String.join(", ", REQUIRED_HEADERS));
        }
        java.util.List<String> missing = REQUIRED_HEADERS.stream()
                .filter(h -> !headers.containsKey(h))
                .sorted()
                .toList();
        if ("UPDATE".equalsIgnoreCase(mode) && !headers.containsKey("externalCode")) {
            missing = new java.util.ArrayList<>(missing);
            missing.add("externalCode (обязателен для режима UPDATE)");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("В CSV отсутствуют обязательные колонки: "
                    + String.join(", ", missing));
        }
    }

    private void validateRecord(CSVRecord rec, String mode) {
        if ("UPDATE".equalsIgnoreCase(mode) && (rec.get("externalCode") == null || rec.get("externalCode").isBlank()))
            throw new IllegalArgumentException("externalCode обязателен для режима UPDATE");
        if (rec.get("trainNumber").isBlank()) throw new IllegalArgumentException("trainNumber пуст");
        if (LocalDate.parse(rec.get("departureDate")).isBefore(LocalDate.now()))
            throw new IllegalArgumentException("departureDate в прошлом");
        LocalTime.parse(rec.get("departureTime"));
        if (Integer.parseInt(rec.get("wagonNumber")) < 1) throw new IllegalArgumentException("wagonNumber < 1");
        if (Integer.parseInt(rec.get("seatNumber")) < 1) throw new IllegalArgumentException("seatNumber < 1");
        if (new BigDecimal(rec.get("price")).compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("price <= 0");
    }

    private void applyRecord(CSVRecord rec) {
        Booking b = new Booking();
        applyRecordTo(b, rec);
        bookings.save(b);
    }

    /**
     * Переносит поля строки CSV в сущность. Используется и при создании,
     * и при обновлении — раньше внешний код из CSV игнорировался, из-за чего
     * режим UPDATE не мог найти запись и создавал дубликат.
     */
    private void applyRecordTo(Booking b, CSVRecord rec) {
        String passengerEmail = rec.get("passengerEmail");
        Passenger p = passengers.findAll().stream()
                .filter(x -> x.getEmail().equalsIgnoreCase(passengerEmail))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + passengerEmail));
        b.setPassenger(p);
        // Внешний код из CSV сохраняем, иначе UPDATE никогда не найдёт запись
        if (rec.isMapped("externalCode") && !rec.get("externalCode").isBlank())
            b.setExternalCode(rec.get("externalCode"));
        b.setTrainNumber(rec.get("trainNumber"));
        b.setRouteFrom(rec.get("routeFrom"));
        b.setRouteTo(rec.get("routeTo"));
        b.setDepartureDate(LocalDate.parse(rec.get("departureDate")));
        b.setDepartureTime(LocalTime.parse(rec.get("departureTime")));
        b.setWagonNumber(Integer.parseInt(rec.get("wagonNumber")));
        b.setSeatNumber(Integer.parseInt(rec.get("seatNumber")));
        b.setPrice(new BigDecimal(rec.get("price")));
        if (rec.isMapped("status") && !rec.get("status").isBlank())
            b.setStatus(ru.mirea.railways.domain.BookingStatus.parse(rec.get("status")));
        if (rec.isMapped("paid") && !rec.get("paid").isBlank())
            b.setPaid(Boolean.parseBoolean(rec.get("paid")));
    }
}