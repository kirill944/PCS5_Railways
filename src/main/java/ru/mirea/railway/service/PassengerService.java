package ru.mirea.railway.service;

import static ru.mirea.railway.util.Gradient.between;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Бизнес-логика работы с пассажирами.
 *
 * Правила:
 *   - телефон строго +7XXXXXXXXXX;
 *   - дата рождения в прошлом, возраст не более 150 лет.
 */
public class PassengerService {

    private static final int[] PINK   = {255, 105, 180};
    private static final int[] VIOLET = {138, 43, 226};

    /** Телефон: строго +7 и ровно 10 цифр. */
    private static final String PHONE_REGEX = "^\\+7\\d{10}$";

    /** Максимальный возраст пассажира. */
    private static final int MAX_AGE = 150;

    private final PassengerRepository repository;

    public PassengerService() {
        this.repository = new PassengerRepositoryJdbc();
    }

    public PassengerService(PassengerRepository repository) {
        this.repository = repository;
    }

    //  CREATE

    public Passenger create(Passenger passenger) {
        validate(passenger);
        checkUniqueness(passenger, null);
        return repository.save(passenger);
    }

    //  UPDATE

    public void update(Passenger passenger) {
        if (passenger.getId() == null) {
            throw new BusinessException(between("Не указан ID пассажира для обновления", PINK, VIOLET));
        }
        if (!repository.existsById(passenger.getId())) {
            throw new EntityNotFoundException("Пассажир", passenger.getId());
        }
        validate(passenger);
        checkUniqueness(passenger, passenger.getId());
        repository.update(passenger);
    }

    //  DELETE

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Пассажир", id);
        }
        repository.deleteById(id);
    }

    //  READ

    public Passenger findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пассажир", id));
    }

    public List<Passenger> findAll() {
        return repository.findAll();
    }

    public List<Passenger> searchByFullName(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            throw new BusinessException(between("Фрагмент ФИО не может быть пустым", PINK, VIOLET));
        }
        return repository.findByFullNameLike(fragment);
    }

    public Optional<Passenger> findByPassport(String passportNumber) {
        return repository.findByPassportNumber(passportNumber);
    }

    public Optional<Passenger> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public long count() {
        return repository.count();
    }

    //  Внутренние проверки

    private void validate(Passenger p) {
        if (p == null) {
            throw new BusinessException(between("Пассажир не может быть null", PINK, VIOLET));
        }

        if (p.getFullName() == null
                || !p.getFullName().matches("^[А-Яа-яЁёA-Za-z\\s-]{3,150}$")) {
            throw new BusinessException(between(
                    "Неверный формат ФИО: " + p.getFullName()
                            + " (допускаются буквы, пробелы и дефис, от 3 символов)", PINK, VIOLET));
        }

        if (p.getPassportNumber() == null
                || !p.getPassportNumber().matches("^\\d{4}\\s\\d{6}$")) {
            throw new BusinessException(between(
                    "Неверный формат паспорта: " + p.getPassportNumber()
                            + " (Ожидается формат: серия (4 цифры) номер (6 цифр), например: 1234 123456", PINK, VIOLET));
        }

        if (p.getEmail() == null
                || !p.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            throw new BusinessException(between(
                    "Некорректный email: " + p.getEmail(), PINK, VIOLET));
        }

        if (p.getPhone() == null || !p.getPhone().matches(PHONE_REGEX)) {
            throw new BusinessException(between(
                    "Некорректный телефон: " + p.getPhone()
                            + " (ожидается российский номер в формате '+7XXXXXXXXXX')", PINK, VIOLET));
        }

        if (p.getBirthDate() == null) {
            throw new BusinessException(between("Дата рождения обязательна", PINK, VIOLET));
        }

        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(MAX_AGE);

        if (!p.getBirthDate().isBefore(today)) {
            throw new BusinessException(between(
                    "Дата рождения должна быть в прошлом: " + p.getBirthDate(), PINK, VIOLET));
        }
        if (p.getBirthDate().isBefore(minDate)) {
            throw new BusinessException(between(
                    "Возраст не может превышать " + MAX_AGE
                            + " лет (дата рождения не раньше " + minDate + ")", PINK, VIOLET));
        }
    }

    /** Проверка уникальности email и паспорта. excludeId — при обновлении. */
    private void checkUniqueness(Passenger p, Long excludeId) {
        repository.findByEmail(p.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException(between(
                        "Пассажир с email '" + p.getEmail() + "' уже существует", PINK, VIOLET));
            }
        });
        repository.findByPassportNumber(p.getPassportNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException(between(
                        "Пассажир с паспортом '" + p.getPassportNumber() + "' уже существует", PINK, VIOLET));
            }
        });
    }
}