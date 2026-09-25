package ru.mirea.railway.repository;

import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Контракт доступа к данным бронирований.
 */
public interface BookingRepository {

    // CRUD

    Booking save(Booking booking);

    void update(Booking booking);

    void deleteById(Long id);

    Optional<Booking> findById(Long id);

    List<Booking> findAll();

    // Поиск

    /** Поиск по номеру поезда (частичное совпадение). */
    List<Booking> findByTrainNumber(String trainNumber);

    /** Поиск по маршруту: станция отправления и/или назначения. */
    List<Booking> findByRoute(String from, String to);

    /** Поиск по ФИО пассажира (JOIN с passengers). */
    List<Booking> findByPassengerFullName(String fragment);

    /** Поиск по номеру паспорта пассажира (JOIN). */
    List<Booking> findByPassengerPassport(String passportNumber);

    // Фильтрация

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByPassengerId(Long passengerId);

    List<Booking> findByDepartureDateBetween(LocalDate from, LocalDate to);

    // Проверки

    /** Занято ли место на конкретный поезд/вагон/дату. */
    boolean isSeatTaken(String trainNumber, int wagonNumber,
                        int seatNumber, LocalDate departureDate);

    boolean existsById(Long id);

    // Статистика

    long count();

    long countByStatus(BookingStatus status);
}