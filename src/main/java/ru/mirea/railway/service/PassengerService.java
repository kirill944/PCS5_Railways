package ru.mirea.railway.service;

import ru.mirea.railway.exception.BusinessException;
import ru.mirea.railway.exception.EntityNotFoundException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.repository.impl.PassengerRepositoryJdbc;
import ru.mirea.railway.validation.PassengerValidator;
import ru.mirea.railway.validation.ValidationErrors;

import java.util.List;
import java.util.Optional;

/**
 * Бизнес-логика работы с пассажирами.
 *
 * Формат полей проверяет PassengerValidator; здесь — то,
 * для чего нужна БД: уникальность паспорта и email.
 * При ошибке бросается ValidationException с указанием поля.
 */
public class PassengerService {

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
            throw new BusinessException("Не указан ID пассажира для обновления");
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
            throw new BusinessException("Введите фрагмент ФИО для поиска");
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
            throw new BusinessException("Пассажир не может быть null");
        }
        PassengerValidator.validate(p);
    }

    /** Проверка уникальности email и паспорта. excludeId — при обновлении. */
    private void checkUniqueness(Passenger p, Long excludeId) {
        ValidationErrors errors = new ValidationErrors();
        repository.findByPassportNumber(p.getPassportNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                errors.add(PassengerValidator.PASSPORT, "пассажир с паспортом "
                        + p.getPassportNumber() + " уже зарегистрирован (" + existing.getFullName()
                        + "). Проверьте серию и номер.");
            }
        });
        repository.findByEmail(p.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                errors.add(PassengerValidator.EMAIL, "адрес " + p.getEmail()
                        + " уже используется другим пассажиром. Укажите другой email.");
            }
        });
        errors.throwIfAny();
    }
}
