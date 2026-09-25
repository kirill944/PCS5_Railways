package ru.mirea.railway.service;

import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.repository.BookingRepository;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.BookingRepositoryJdbc;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Статистика системы — 7 показателей.
 */
public class StatisticsService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;

    public StatisticsService() {
        this.bookingRepository = new BookingRepositoryJdbc();
        this.passengerRepository = new PassengerRepositoryJdbc();
    }

    public StatisticsService(BookingRepository bookingRepository,
                             PassengerRepository passengerRepository) {
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
    }

    /** DTO для статистики — иммутабельный снимок. */
    public record Statistics(
            long totalPassengers,
            long totalBookings,
            long activeBookings,
            long completedBookings,
            long cancelledBookings,
            BigDecimal averagePrice,
            Map<String, Long> bookingsByTrain
    ) {}

    public Statistics collect() {
        long totalPassengers = passengerRepository.count();
        long totalBookings   = bookingRepository.count();

        long active = bookingRepository.countByStatus(BookingStatus.CREATED)
                + bookingRepository.countByStatus(BookingStatus.CONFIRMED)
                + bookingRepository.countByStatus(BookingStatus.PAID);

        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelled = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        List<Booking> all = bookingRepository.findAll();

        BigDecimal avgPrice = all.stream()
                .map(Booking::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!all.isEmpty()) {
            avgPrice = avgPrice.divide(
                    BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);
        }

        // Группировка по номеру поезда — Stream API
        Map<String, Long> byTrain = all.stream()
                .collect(Collectors.groupingBy(
                        Booking::getTrainNumber,
                        TreeMap::new,
                        Collectors.counting()));

        return new Statistics(
                totalPassengers,
                totalBookings,
                active,
                completed,
                cancelled,
                avgPrice,
                byTrain);
    }
}