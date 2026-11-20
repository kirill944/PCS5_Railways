package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import ru.mirea.railways.domain.Booking;
import ru.mirea.railways.dto.AnalyticsResponse;
import ru.mirea.railways.repository.BookingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {
    private final BookingRepository repo;
    public AnalyticsService(BookingRepository repo) { this.repo = repo; }

    public AnalyticsResponse summary() {
        List<Booking> all = repo.findAll();
        long total = all.size();
        long archived = all.stream().filter(Booking::isArchived).count();
        long active = total - archived;
        long paid = all.stream().filter(Booking::isPaid).count();
        long unpaid = total - paid;
        BigDecimal avg = total == 0 ? BigDecimal.ZERO :
                all.stream().map(Booking::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        Map<String, Long> byStatus = new HashMap<>();
        Map<String, Long> byRisk = new HashMap<>();
        Map<String, Long> byPassenger = new HashMap<>();
        for (Booking b : all) {
            byStatus.merge(b.getStatus().name(), 1L, Long::sum);
            byRisk.merge(b.getRiskLevel().name(), 1L, Long::sum);
            byPassenger.merge(b.getPassenger().getFullName(), 1L, Long::sum);
        }
        return new AnalyticsResponse(total, active, archived, paid, unpaid, avg,
                byStatus, byRisk, byPassenger);
    }
}