package ru.mirea.railway.service;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.repository.BookingRepository;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.BookingRepositoryJdbc;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;
import ru.mirea.railway.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Бизнес-логика работы с бронированиями.
 */
public class BookingService {

    private static final int MAX_WAGON = 20;
    private static final int MAX_SEAT  = 50;

    private static final BigDecimal MIN_PRICE = new BigDecimal("1000");
    private static final BigDecimal MAX_PRICE = new BigDecimal("100000");

    /** Горизонт бронирования — не более 1 года вперёд. */
    private static final int MAX_DAYS_AHEAD = 365;

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

        if (!passengerRepository.existsById(booking.getPassengerId())) {
            throw new EntityNotFoundException("Пассажир", booking.getPassengerId());
        }

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
    //  СМЕНА СТАТУСА
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
    //  СОРТИРОВКА
    // =========================================================

    public List<Booking> sortByDepartureDate(boolean ascending) {
        Comparator<Booking> cmp = Comparator.comparing(Booking::getDepartureDate)
                .thenComparing(Booking::getDepartureTime);
        if (!ascending) {
            cmp = cmp.reversed();
        }
        return findAll().stream().sorted(cmp).collect(Collectors.toList());
    }

    public List<Booking> sortByPrice(boolean ascending) {
        Comparator<Booking> cmp = Comparator.comparing(Booking::getPrice);
        if (!ascending) {
            cmp = cmp.reversed();
        }
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
    //  Внутренние проверки
    // =========================================================

    private void validateCommon(Booking b) {
        if (b == null) {
            throw new BusinessException("Бронь не может быть null");
        }
        if (b.getPassengerId() == null) {
            throw new BusinessException("Не указан пассажир");
        }

        if (b.getTrainNumber() == null
                || !b.getTrainNumber().matches(InputValidator.TRAIN_REGEX)) {
            throw new BusinessException(
                    "Неверный номер поезда: " + b.getTrainNumber()
                            + ". " + InputValidator.TRAIN_ERROR);
        }

        if (b.getRouteFrom() == null || b.getRouteFrom().isBlank()) {
            throw new BusinessException("Станция отправления обязательна");
        }
        if (b.getRouteTo() == null || b.getRouteTo().isBlank()) {
            throw new BusinessException("Станция назначения обязательна");
        }
        if (b.getRouteFrom().equalsIgnoreCase(b.getRouteTo())) {
            throw new BusinessException(
                    "Станция отправления и назначения не могут совпадать");
        }

        if (b.getDepartureDate() == null || b.getDepartureTime() == null) {
            throw new BusinessException("Дата и время отправления обязательны");
        }

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(MAX_DAYS_AHEAD);

        if (b.getDepartureDate().isBefore(today)) {
            throw new BusinessException(
                    "Дата отправления не может быть в прошлом: " + b.getDepartureDate());
        }
        if (b.getDepartureDate().isAfter(maxDate)) {
            throw new BusinessException(
                    "Дата отправления не может быть позже " + maxDate
                            + " (не более 1 года вперёд)");
        }

        if (b.getWagonNumber() < 1 || b.getWagonNumber() > MAX_WAGON) {
            throw new BusinessException(
                    "Номер вагона должен быть от 1 до " + MAX_WAGON);
        }
        if (b.getSeatNumber() < 1 || b.getSeatNumber() > MAX_SEAT) {
            throw new BusinessException(
                    "Номер места должен быть от 1 до " + MAX_SEAT);
        }

        if (b.getPrice() == null || b.getPrice().compareTo(MIN_PRICE) < 0) {
            throw new BusinessException(
                    "Цена билета не может быть меньше " + MIN_PRICE + " ₽");
        }
        if (b.getPrice().compareTo(MAX_PRICE) > 0) {
            throw new BusinessException(
                    "Цена билета не может быть больше " + MAX_PRICE + " ₽");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Поле '" + fieldName + "' обязательно");
        }
    }
}