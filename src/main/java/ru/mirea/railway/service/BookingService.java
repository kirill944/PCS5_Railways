package ru.mirea.railway.service;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.repository.BookingRepository;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.BookingRepositoryJdbc;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Бизнес-логика работы с бронированиями.
 *
 * Бизнес-правила:
 *   BR-1. Нельзя создать бронь без существующего пассажира.
 *   BR-2. Нельзя занять уже занятое место (поезд + вагон + место + дата).
 *   BR-3. Номер вагона и места >= 1.
 *   BR-4. Дата отправления не может быть в прошлом.
 *   BR-5. Цена билета > 0.
 *   BR-6. Запрещённые переходы статусов (см. BookingStatus.canTransitionTo).
 */
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;

    public BookingService() {
        this.bookingRepository = new BookingRepositoryJdbc();
        this.passengerRepository = new PassengerRepositoryJdbc();
    }

    public BookingService(BookingRepository bookingRepository,
                          PassengerRepository passengerRepository) {
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
    }

    // =========================================================
    //  CREATE
    // =========================================================

    public Booking create(Booking booking) {
        validateCommon(booking);

        // BR-1: пассажир должен существовать
        if (!passengerRepository.existsById(booking.getPassengerId())) {
            throw new EntityNotFoundException("Пассажир", booking.getPassengerId());
        }

        // BR-2: место не должно быть занято
        if (bookingRepository.isSeatTaken(
                booking.getTrainNumber(),
                booking.getWagonNumber(),
                booking.getSeatNumber(),
                booking.getDepartureDate())) {
            throw new BusinessException(String.format(
                    "Место %d в вагоне %d на поезд %s (%s) уже занято",
                    booking.getSeatNumber(), booking.getWagonNumber(),
                    booking.getTrainNumber(), booking.getDepartureDate()));
        }

        // Новая бронь всегда создаётся в статусе CREATED
        booking.setStatus(BookingStatus.CREATED);

        return bookingRepository.save(booking);
    }

    // =========================================================
    //  UPDATE
    // =========================================================

    public void update(Booking booking) {
        if (booking.getId() == null) {
            throw new BusinessException("Не указан ID брони для обновления");
        }
        Booking existing = findById(booking.getId());

        validateCommon(booking);

        if (!passengerRepository.existsById(booking.getPassengerId())) {
            throw new EntityNotFoundException("Пассажир", booking.getPassengerId());
        }

        // Проверка занятости места — только если место/поезд/дата изменились
        boolean seatChanged =
                !existing.getTrainNumber().equals(booking.getTrainNumber())
                        || existing.getWagonNumber() != booking.getWagonNumber()
                        || existing.getSeatNumber() != booking.getSeatNumber()
                        || !existing.getDepartureDate().equals(booking.getDepartureDate());

        if (seatChanged && bookingRepository.isSeatTaken(
                booking.getTrainNumber(),
                booking.getWagonNumber(),
                booking.getSeatNumber(),
                booking.getDepartureDate())) {
            throw new BusinessException("Новое место уже занято");
        }

        bookingRepository.update(booking);
    }

    // =========================================================
    //  DELETE
    // =========================================================

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Бронирование", id);
        }
        bookingRepository.deleteById(id);
    }

    // =========================================================
    //  READ
    // =========================================================

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование", id));
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    // =========================================================
    //  СМЕНА СТАТУСА (BR-6)
    // =========================================================

    public void changeStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = findById(bookingId);
        BookingStatus current = booking.getStatus();

        if (!current.canTransitionTo(newStatus)) {
            throw new BusinessException(String.format(
                    "Недопустимый переход статуса: %s -> %s", current, newStatus));
        }
        booking.setStatus(newStatus);
        bookingRepository.update(booking);
    }

    // =========================================================
    //  ПОИСК
    // =========================================================

    public List<Booking> searchByTrainNumber(String trainNumber) {
        requireNonBlank(trainNumber, "Номер поезда");
        return bookingRepository.findByTrainNumber(trainNumber);
    }

    public List<Booking> searchByRoute(String from, String to) {
        if ((from == null || from.isBlank()) && (to == null || to.isBlank())) {
            throw new BusinessException("Укажите хотя бы одну станцию для поиска");
        }
        return bookingRepository.findByRoute(from, to);
    }

    public List<Booking> searchByPassengerName(String fragment) {
        requireNonBlank(fragment, "ФИО пассажира");
        return bookingRepository.findByPassengerFullName(fragment);
    }

    public List<Booking> searchByPassengerPassport(String passport) {
        requireNonBlank(passport, "Номер паспорта");
        return bookingRepository.findByPassengerPassport(passport);
    }

    // =========================================================
    //  ФИЛЬТРАЦИЯ
    // =========================================================

    public List<Booking> filterByStatus(BookingStatus status) {
        if (status == null) {
            throw new BusinessException("Статус обязателен для фильтрации");
        }
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> filterByPassenger(Long passengerId) {
        if (passengerId == null) {
            throw new BusinessException("ID пассажира обязателен");
        }
        return bookingRepository.findByPassengerId(passengerId);
    }

    public List<Booking> filterByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Обе даты обязательны");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Дата 'от' не может быть позже даты 'до'");
        }
        return bookingRepository.findByDepartureDateBetween(from, to);
    }

    // =========================================================
    //  СОРТИРОВКА (Java Stream API + Comparator)
    // =========================================================

    public List<Booking> sortByDepartureDate(boolean ascending) {
        Comparator<Booking> cmp = Comparator.comparing(Booking::getDepartureDate)
                .thenComparing(Booking::getDepartureTime);
        if (!ascending) cmp = cmp.reversed();
        return findAll().stream().sorted(cmp).collect(Collectors.toList());
    }

    public List<Booking> sortByPrice(boolean ascending) {
        Comparator<Booking> cmp = Comparator.comparing(Booking::getPrice);
        if (!ascending) cmp = cmp.reversed();
        return findAll().stream().sorted(cmp).collect(Collectors.toList());
    }

    public List<Booking> sortByTrainNumber() {
        return findAll().stream()
                .sorted(Comparator.comparing(Booking::getTrainNumber))
                .collect(Collectors.toList());
    }

    public List<Booking> sortByStatus() {
        return findAll().stream()
                .sorted(Comparator.comparing(Booking::getStatus))
                .collect(Collectors.toList());
    }

    // =========================================================
    //  Внутренние проверки (BR-3, BR-4, BR-5)
    // =========================================================

    private void validateCommon(Booking b) {
        if (b == null) {
            throw new BusinessException("Бронь не может быть null");
        }
        if (b.getPassengerId() == null) {
            throw new BusinessException("Не указан пассажир");
        }
        requireNonBlank(b.getTrainNumber(), "Номер поезда");
        requireNonBlank(b.getRouteFrom(), "Станция отправления");
        requireNonBlank(b.getRouteTo(), "Станция назначения");

        if (b.getRouteFrom().equalsIgnoreCase(b.getRouteTo())) {
            throw new BusinessException(
                    "Станция отправления и назначения не могут совпадать");
        }
        if (b.getDepartureDate() == null || b.getDepartureTime() == null) {
            throw new BusinessException("Дата и время отправления обязательны");
        }

        // BR-4
        if (b.getDepartureDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Дата отправления не может быть в прошлом: " + b.getDepartureDate());
        }

        // BR-3
        if (b.getWagonNumber() < 1) {
            throw new BusinessException("Номер вагона должен быть >= 1");
        }
        if (b.getSeatNumber() < 1) {
            throw new BusinessException("Номер места должен быть >= 1");
        }

        // BR-5
        if (b.getPrice() == null || b.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цена билета должна быть > 0");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Поле '" + fieldName + "' обязательно");
        }
    }
}