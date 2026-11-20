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
            for (CSVRecord rec : parser) {
                try {
                    if ("UPDATE".equalsIgnoreCase(mode)) {
                        String code = rec.get("externalCode");
                        Optional<Booking> existing = bookings.findAll().stream()
                                .filter(b -> b.getExternalCode().equals(code)).findFirst();
                        if (existing.isPresent()) { updated++; continue; }
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
        String passengerEmail = rec.get("passengerEmail");
        Passenger p = passengers.findAll().stream()
                .filter(x -> x.getEmail().equalsIgnoreCase(passengerEmail))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + passengerEmail));
        Booking b = new Booking();
        b.setPassenger(p);
        b.setTrainNumber(rec.get("trainNumber"));
        b.setRouteFrom(rec.get("routeFrom"));
        b.setRouteTo(rec.get("routeTo"));
        b.setDepartureDate(LocalDate.parse(rec.get("departureDate")));
        b.setDepartureTime(LocalTime.parse(rec.get("departureTime")));
        b.setWagonNumber(Integer.parseInt(rec.get("wagonNumber")));
        b.setSeatNumber(Integer.parseInt(rec.get("seatNumber")));
        b.setPrice(new BigDecimal(rec.get("price")));
        if (rec.isMapped("status") && !rec.get("status").isBlank())
            b.setStatus(ru.mirea.railways.domain.BookingStatus.valueOf(rec.get("status")));
        if (rec.isMapped("paid")) b.setPaid(Boolean.parseBoolean(rec.get("paid")));
        bookings.save(b);
    }
}