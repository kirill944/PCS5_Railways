package ru.mirea.railways.dto;
import java.math.BigDecimal;
import java.util.Map;
public record AnalyticsResponse(
        long totalCount,
        long activeCount,
        long archivedCount,
        long paidCount,
        long unpaidCount,
        BigDecimal averagePrice,
        Map<String, Long> byStatus,
        Map<String, Long> byRiskLevel,
        Map<String, Long> byPassenger
) {}