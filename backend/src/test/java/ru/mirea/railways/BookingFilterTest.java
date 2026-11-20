package ru.mirea.railways;

import org.junit.jupiter.api.Test;
import ru.mirea.railways.domain.BookingRiskLevel;
import ru.mirea.railways.filter.BookingFilter;

import static org.junit.jupiter.api.Assertions.*;

class BookingFilterTest {
    @Test void hasSearchBlank() {
        assertFalse(new BookingFilter(" ", null, null, null, null, null, null, null, null, null, null).hasSearch());
    }
    @Test void hasSearchNonBlank() {
        assertTrue(new BookingFilter("train", null, null, BookingRiskLevel.LOW, null, null, null, null, null, null, null).hasSearch());
    }
}   