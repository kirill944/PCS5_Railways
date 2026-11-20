package ru.mirea.railways;

import org.junit.jupiter.api.Test;
import ru.mirea.railways.domain.*;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingServiceTest {

    @Test void riskIsHighForFreshUnpaidBookingNearDeparture() {
        Booking b = new Booking();
        b.setStatus(BookingStatus.CREATED);
        b.setDepartureDate(LocalDate.now());
        assertEquals(BookingRiskLevel.HIGH, b.getRiskLevel());
    }

    @Test void riskIsLowForPaidBooking() {
        Booking b = new Booking();
        b.setStatus(BookingStatus.PAID);
        b.setDepartureDate(LocalDate.now().plusDays(1));
        assertEquals(BookingRiskLevel.LOW, b.getRiskLevel());
    }
}