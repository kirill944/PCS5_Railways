package ru.mirea.railway.service;

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

    // =========================================================
    //  CREATE
    // =========================================================

    public Passenger create(Passenger passenger) {
        validate(passenger);
        checkUniqueness(passenger, null);
        return repository.save(passenger);
    }

    // =========================================================
    //  UPDATE
    // =========================================================

    public void update(Passenger passenger) {
        if (passenger.getId() == null) {
            throw new BusinessException("Не указан ID пассажира для обновления");
        }
        if (!repository.existsById(passenger.getId())) {
            throw new EntityNotFoundException("Пассажир", passenger.getId());
        }
        validate(passenger);
        checkUniqueness(passenger, passenger.getId());
        repository.update(passenger);
    }

    // =========================================================
    //  DELETE
    // =========================================================

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Пассажир", id);
        }
        repository.deleteById(id);
    }

    // =========================================================
    //  READ
    // =========================================================

    public Passenger findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пассажир", id));
    }

    public List<Passenger> findAll() {
        return repository.findAll();
    }

    public List<Passenger> searchByFullName(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            throw new BusinessException("Фрагмент ФИО не может быть пустым");
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

    // =========================================================
    //  Внутренние проверки
    // =========================================================

    private void validate(Passenger p) {
        if (p == null) {
            throw new BusinessException("Пассажир не может быть null");
        }

        if (p.getFullName() == null
                || !p.getFullName().matches("^[А-Яа-яЁёA-Za-z\\s-]{3,150}$")) {
            throw new BusinessException(
                    "Неверный формат ФИО: " + p.getFullName()
                            + " (допускаются буквы, пробелы и дефис, от 3 символов)");
        }

        if (p.getPassportNumber() == null
                || !p.getPassportNumber().matches("^\\d{4}\\s\\d{6}$")) {
            throw new BusinessException(
                    "Неверный формат паспорта: " + p.getPassportNumber()
                            + " (Ожидается формат: серия (4 цифры) номер (6 цифр), например: 1234 123456");
        }

        if (p.getEmail() == null
                || !p.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            throw new BusinessException("Некорректный email: " + p.getEmail());
        }

        if (p.getPhone() == null || !p.getPhone().matches(PHONE_REGEX)) {
            throw new BusinessException(
                    "Некорректный телефон: " + p.getPhone()
                            + " (ожидается российский номер в формате '+7XXXXXXXXXX')");
        }

        if (p.getBirthDate() == null) {
            throw new BusinessException("Дата рождения обязательна");
        }

        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(MAX_AGE);

        if (!p.getBirthDate().isBefore(today)) {
            throw new BusinessException(
                    "Дата рождения должна быть в прошлом: " + p.getBirthDate());
        }
        if (p.getBirthDate().isBefore(minDate)) {
            throw new BusinessException(
                    "Возраст не может превышать " + MAX_AGE
                            + " лет (дата рождения не раньше " + minDate + ")");
        }
    }

    /** Проверка уникальности email и паспорта. excludeId — при обновлении. */
    private void checkUniqueness(Passenger p, Long excludeId) {
        repository.findByEmail(p.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Пассажир с email '" + p.getEmail() + "' уже существует");
            }
        });
        repository.findByPassportNumber(p.getPassportNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Пассажир с паспортом '" + p.getPassportNumber() + "' уже существует");
            }
        });
    }
}