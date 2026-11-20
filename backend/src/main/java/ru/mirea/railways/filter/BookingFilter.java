package ru.mirea.railways.filter;

import ru.mirea.railways.domain.BookingRiskLevel;
import ru.mirea.railways.domain.BookingStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingFilter(
        String search,
        UUID passengerId,
        BookingStatus status,
        BookingRiskLevel riskLevel,
        Boolean paid,
        Boolean archived,
        String tag,
        String priceOp,
        BigDecimal priceValue,
        String sortBy,
        String sortDir
) {
    public boolean hasSearch() { return search != null && !search.isBlank(); }
}