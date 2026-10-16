package ru.mirea.railway.service;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.exception.ValidationException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.repository.BookingRepository;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.BookingRepositoryJdbc;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;
import ru.mirea.railway.validation.BookingValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Бизнес-логика работы с бронированиями.
 *
 * Формат полей проверяет BookingValidator; здесь — то,
 * для чего нужна БД:
 *   - пассажир существует;
 *   - место на поезд/вагон/дату уникально (кроме CANCELLED);
 *   - смена статуса допустима по жизненному циклу брони.
 * При ошибке бросается ValidationException с указанием поля.
 */
public class BookingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        validateCommon(booking, true);
        requirePassengerExists(booking.getPassengerId());
        requireSeatFree(booking);

        booking.setStatus(BookingStatus.CREATED);
        return bookingRepository.save(booking);
    }

    //  UPDATE

    public void update(Booking booking) {
        if (booking.getId() == null) {
            throw new BusinessException("Не указан ID брони для обновления");
        }
        Booking existing = findById(booking.getId());

        // Окно «не в прошлом и не дальше года» проверяем, только если
        // дату/время меняют — иначе нельзя исправить прошедшую поездку.
        boolean departureChanged =
                !existing.getDepartureDate().equals(booking.getDepartureDate())
                        || !existing.getDepartureTime().equals(booking.getDepartureTime());
        validateCommon(booking, departureChanged);
        requirePassengerExists(booking.getPassengerId());

        boolean seatChanged =
                !existing.getTrainNumber().equals(booking.getTrainNumber())
                        || existing.getWagonNumber() != booking.getWagonNumber()
                        || existing.getSeatNumber() != booking.getSeatNumber()
                        || !existing.getDepartureDate().equals(booking.getDepartureDate());
        if (seatChanged) {
            requireSeatFree(booking);
        }

        BookingStatus from = existing.getStatus();
        BookingStatus to = booking.getStatus();
        if (from != to && !from.canTransitionTo(to)) {
            throw new ValidationException(BookingValidator.STATUS,
                    "нельзя сменить статус «" + from.getDisplayName() + "» на «"
                            + to.getDisplayName() + "». " + allowedTransitions(from));
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
            throw new BusinessException("Нельзя сменить статус «" + current.getDisplayName()
                    + "» на «" + newStatus.getDisplayName() + "». " + allowedTransitions(current));
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

    //  ФИЛЬТРАЦИЯ

    public List<Booking> filterByStatus(BookingStatus status) {
        if (status == null) {
            throw new BusinessException("Выберите статус для фильтрации");
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
            throw new BusinessException("Для фильтра по датам укажите обе даты: «с» и «по»");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Дата «с» не может быть позже даты «по»");
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

    private void validateCommon(Booking b, boolean checkDepartureWindow) {
        if (b == null) {
            throw new BusinessException("Бронь не может быть null");
        }
        BookingValidator.validate(b, checkDepartureWindow);
    }

    /** Ссылка на связанную сущность: пассажир должен существовать в БД. */
    private void requirePassengerExists(Long passengerId) {
        if (!passengerRepository.existsById(passengerId)) {
            throw new ValidationException(BookingValidator.PASSENGER,
                    "пассажир с ID=" + passengerId + " не найден (возможно, он удалён). "
                            + "Обновите список и выберите пассажира заново.");
        }
    }

    private void requireSeatFree(Booking b) {
        if (bookingRepository.isSeatTaken(b.getTrainNumber(), b.getWagonNumber(),
                b.getSeatNumber(), b.getDepartureDate())) {
            throw new ValidationException(BookingValidator.SEAT, String.format(
                    "место %d в вагоне %d поезда %s на %s уже занято. Выберите другое место или вагон.",
                    b.getSeatNumber(), b.getWagonNumber(), b.getTrainNumber(),
                    b.getDepartureDate().format(DATE_FMT)));
        }
    }

    private static String allowedTransitions(BookingStatus from) {
        String allowed = Arrays.stream(BookingStatus.values())
                .filter(from::canTransitionTo)
                .map(s -> "«" + s.getDisplayName() + "»")
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
        return allowed == null
                ? "Бронь в статусе «" + from.getDisplayName() + "» изменить нельзя."
                : "Допустимые варианты: " + allowed + ".";
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Заполните поле «" + fieldName + "»");
        }
    }
}
