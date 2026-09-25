package ru.mirea.railway.repository;

import ru.mirea.railway.model.Passenger;

import java.util.List;
import java.util.Optional;

/**
 * Контракт доступа к данным пассажиров.
 * Реализация может быть JDBC, in-memory, REST — Service не знает деталей.
 */
public interface PassengerRepository {

    /** Сохраняет нового пассажира, возвращает с присвоенным ID. */
    Passenger save(Passenger passenger);

    /** Обновляет существующего пассажира. */
    void update(Passenger passenger);

    /** Удаляет пассажира по ID. */
    void deleteById(Long id);

    /** Ищет пассажира по ID. */
    Optional<Passenger> findById(Long id);

    /** Возвращает всех пассажиров. */
    List<Passenger> findAll();

    /** Поиск по ФИО (частичное совпадение, регистронезависимо). */
    List<Passenger> findByFullNameLike(String fragment);

    /** Поиск по номеру паспорта (точное совпадение). */
    Optional<Passenger> findByPassportNumber(String passportNumber);

    /** Поиск по email (точное совпадение). */
    Optional<Passenger> findByEmail(String email);

    /** Проверка существования пассажира по ID. */
    boolean existsById(Long id);

    /** Общее количество пассажиров. */
    long count();
}