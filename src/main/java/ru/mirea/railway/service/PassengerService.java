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
 *   1. ФИО, паспорт, email, телефон, дата рождения обязательны.
 *   2. Email уникален и должен содержать '@'.
 *   3. Номер паспорта уникален.
 *   4. Телефон — только цифры, '+' и длина 10–15.
 *   5. Дата рождения в прошлом.
 */
public class PassengerService {

    private final PassengerRepository repository;

    public PassengerService() {
        this.repository = new PassengerRepositoryJdbc();
    }

    // Для юнит-тестов и подмены реализации
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
    //  Внутренние проверки (бизнес-правила)
    // =========================================================

    private void validate(Passenger p) {
        if (p == null) {
            throw new BusinessException("Пассажир не может быть null");
        }
        requireNonBlank(p.getFullName(), "ФИО");
        requireNonBlank(p.getPassportNumber(), "Номер паспорта");
        requireNonBlank(p.getEmail(), "Email");
        requireNonBlank(p.getPhone(), "Телефон");

        if (p.getFullName().length() < 3) {
            throw new BusinessException("ФИО должно содержать минимум 3 символа");
        }
        if (!p.getEmail().contains("@") || !p.getEmail().contains(".")) {
            throw new BusinessException("Некорректный email: " + p.getEmail());
        }
        if (!p.getPhone().matches("^\\+?[0-9]{10,15}$")) {
            throw new BusinessException(
                    "Некорректный телефон: " + p.getPhone()
                            + " (ожидается 10–15 цифр, возможно с '+')");
        }
        if (p.getBirthDate() == null) {
            throw new BusinessException("Дата рождения обязательна");
        }
        if (!p.getBirthDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Дата рождения должна быть в прошлом: " + p.getBirthDate());
        }
        if (p.getPassportNumber().length() < 5) {
            throw new BusinessException(
                    "Номер паспорта слишком короткий: " + p.getPassportNumber());
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

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Поле '" + fieldName + "' обязательно");
        }
    }
}