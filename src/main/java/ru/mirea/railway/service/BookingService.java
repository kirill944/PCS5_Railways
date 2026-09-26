package ru.mirea.railway.service;

import static ru.mirea.railway.util.Gradient.*;

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
 *
 * Правила:
 *   - номер поезда: 3 цифры + заглавная русская буква (без Ь, Ъ, Ё, Ы);
 *   - станции не совпадают;
 *   - дата отправления: не в прошлом и не позже года вперёд;
 *   - вагон: 1–20;
 *   - место: 1–50;
 *   - цена: 1000–100 000 ₽;
 *   - место на поезд/вагон/дату уникально (кроме CANCELLED).
 */
public class BookingService {

    private static final int MAX_WAGON = 20;
    private static final int MAX_SEAT  = 50;

    private static final BigDecimal MIN_PRICE = new BigDecimal("1000");
    private static final BigDecimal MAX_PRICE = new BigDecimal("100000");

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

    //  CREATE

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
            throw new BusinessException(between(String.format(
                    "Место %d в вагоне %d на поезд %s (%s) уже занято",
                    booking.getSeatNumber(), booking.getWagonNumber(),
                    booking.getTrainNumber(), booking.getDepartureDate()), PINK, VIOLET));
        }

        booking.setStatus(BookingStatus.CREATED);
        return bookingRepository.save(booking);
    }

    //  UPDATE

    public void update(Booking booking) {
        if (booking.getId() == null) {
            throw new BusinessException(between("Не указан ID брони для обновления", PINK, VIOLET));
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
            throw new BusinessException(between("Новое место уже занято", PINK, VIOLET));
        }

        bookingRepository.update(booking);
    }

    //  DELETE

    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Бронирование", id);
        }
        bookingRepository.deleteById(id);
    }

    //  READ

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование", id));
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    /**
     * Публичная проверка занятости места — используется UI
     * для НЕМЕДЛЕННОЙ валидации при вводе вагона/места.
     */
    public boolean isSeatTaken(String trainNumber, int wagonNumber,
                               int seatNumber, LocalDate departureDate) {
        if (trainNumber == null || trainNumber.isBlank() || departureDate == null) {
            return false;
        }
        return bookingRepository.isSeatTaken(trainNumber, wagonNumber, seatNumber, departureDate);
    }

    //  СМЕНА СТАТУСА

    public void changeStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = findById(bookingId);
        BookingStatus current = booking.getStatus();

        if (!current.canTransitionTo(newStatus)) {
            throw new BusinessException(between(String.format(
                    "Недопустимый переход статуса: %s -> %s", current, newStatus), PINK, VIOLET));
        }
        booking.setStatus(newStatus);
        bookingRepository.update(booking);
    }

    //  ПОИСК

    public List<Booking> searchByTrainNumber(String trainNumber) {
        requireNonBlank(trainNumber, "Номер поезда");
        return bookingRepository.findByTrainNumber(trainNumber);
    }

    public List<Booking> searchByRoute(String from, String to) {
        if ((from == null || from.isBlank()) && (to == null || to.isBlank())) {
            throw new BusinessException(between("Укажите хотя бы одну станцию для поиска", PINK, VIOLET));
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

    //  ФИЛЬТРАЦИЯ

    public List<Booking> filterByStatus(BookingStatus status) {
        if (status == null) {
            throw new BusinessException(between("Статус обязателен для фильтрации", PINK, VIOLET));
        }
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> filterByPassenger(Long passengerId) {
        if (passengerId == null) {
            throw new BusinessException(between("ID пассажира обязателен", PINK, VIOLET));
        }
        return bookingRepository.findByPassengerId(passengerId);
    }

    public List<Booking> filterByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(between("Обе даты обязательны", PINK, VIOLET));
        }
        if (from.isAfter(to)) {
            throw new BusinessException(between("Дата 'от' не может быть позже даты 'до'", PINK, VIOLET));
        }
        return bookingRepository.findByDepartureDateBetween(from, to);
    }

    //  СОРТИРОВКА

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

    //  Внутренние проверки

    private void validateCommon(Booking b) {
        if (b == null) {
            throw new BusinessException(between("Бронь не может быть null", PINK, VIOLET));
        }
        if (b.getPassengerId() == null) {
            throw new BusinessException(between("Не указан пассажир", PINK, VIOLET));
        }

        if (b.getTrainNumber() == null
                || !b.getTrainNumber().matches(InputValidator.TRAIN_REGEX)) {
            throw new BusinessException(between(
                    "Неверный номер поезда: " + b.getTrainNumber()
                            + ". " + InputValidator.TRAIN_ERROR, PINK, VIOLET));
        }

        if (b.getRouteFrom() == null || b.getRouteFrom().isBlank()) {
            throw new BusinessException(between("Станция отправления обязательна", PINK, VIOLET));
        }
        if (b.getRouteTo() == null || b.getRouteTo().isBlank()) {
            throw new BusinessException(between("Станция назначения обязательна", PINK, VIOLET));
        }
        if (b.getRouteFrom().equalsIgnoreCase(b.getRouteTo())) {
            throw new BusinessException(between(
                    "Станция отправления и назначения не могут совпадать", PINK, VIOLET));
        }

        if (b.getDepartureDate() == null || b.getDepartureTime() == null) {
            throw new BusinessException(between("Дата и время отправления обязательны", PINK, VIOLET));
        }

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(MAX_DAYS_AHEAD);

        if (b.getDepartureDate().isBefore(today)) {
            throw new BusinessException(between(
                    "Дата отправления не может быть в прошлом: " + b.getDepartureDate(), PINK, VIOLET));
        }
        if (b.getDepartureDate().isAfter(maxDate)) {
            throw new BusinessException(between(
                    "Дата отправления не может быть позже " + maxDate
                            + " (не более 1 года вперёд)", PINK, VIOLET));
        }

        if (b.getWagonNumber() < 1 || b.getWagonNumber() > MAX_WAGON) {
            throw new BusinessException(between(
                    "Номер вагона должен быть от 1 до " + MAX_WAGON, PINK, VIOLET));
        }
        if (b.getSeatNumber() < 1 || b.getSeatNumber() > MAX_SEAT) {
            throw new BusinessException(between(
                    "Номер места должен быть от 1 до " + MAX_SEAT, PINK, VIOLET));
        }

        if (b.getPrice() == null || b.getPrice().compareTo(MIN_PRICE) < 0) {
            throw new BusinessException(between(
                    "Цена билета не может быть меньше " + MIN_PRICE + " ₽", PINK, VIOLET));
        }
        if (b.getPrice().compareTo(MAX_PRICE) > 0) {
            throw new BusinessException(between(
                    "Цена билета не может быть больше " + MAX_PRICE + " ₽", PINK, VIOLET));
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(between(
                    "Поле '" + fieldName + "' обязательно", PINK, VIOLET));
        }
    }
}