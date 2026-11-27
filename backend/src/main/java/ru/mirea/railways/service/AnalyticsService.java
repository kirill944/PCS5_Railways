package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.Booking;
import ru.mirea.railways.dto.AnalyticsResponse;
import ru.mirea.railways.repository.BookingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class AnalyticsService {
    private final BookingRepository repo;
    public AnalyticsService(BookingRepository repo) { this.repo = repo; }

    /**
     * Сводная статистика. Чтение и сборка DTO выполняются внутри транзакции:
     * open-in-view отключён, поэтому ленивые поля (passenger) нельзя трогать
     * после выхода из метода — иначе LazyInitializationException (500).
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        // Одна выборка вместо findAll() на каждую агрегацию
        Map<String, Long> byStatus = new HashMap<>();
        Map<String, Long> byRisk = new HashMap<>();
        Map<String, Long> byPassenger = new HashMap<>();

        long total = 0, archived = 0, paid = 0;
        BigDecimal sum = BigDecimal.ZERO;

        for (Booking b : repo.findAll()) {
            total++;
            if (b.isArchived()) archived++;
            if (b.isPaid()) paid++;
            sum = sum.add(b.getPrice() == null ? BigDecimal.ZERO : b.getPrice());
            byStatus.merge(b.getStatus().name(), 1L, Long::sum);
            byRisk.merge(b.getRiskLevel().name(), 1L, Long::sum);
            byPassenger.merge(b.getPassenger().getFullName(), 1L, Long::sum);
        }

        long active = total - archived;
        long unpaid = total - paid;
        BigDecimal avg = total == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new AnalyticsResponse(total, active, archived, paid, unpaid, avg,
                byStatus, byRisk, byPassenger);
    }
}
