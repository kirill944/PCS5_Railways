package ru.mirea.railway.repository.impl;

import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.model.Passenger;
import ru.mirea.railway.repository.PassengerRepository;
import ru.mirea.railway.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация PassengerRepository.
 * Все запросы параметризованы (PreparedStatement) — защита от SQL-инъекций.
 * Соединения и ResultSet закрываются через try-with-resources.
 */
public class PassengerRepositoryJdbc implements PassengerRepository {

    private final DatabaseManager db;

    public PassengerRepositoryJdbc() {
        this.db = DatabaseManager.getInstance();
    }

    //  CREATE

    @Override
    public Passenger save(Passenger passenger) {
        String sql = """
                INSERT INTO passengers
                    (full_name, passport_number, email, phone, birth_date)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passenger.getFullName());
            ps.setString(2, passenger.getPassportNumber());
            ps.setString(3, passenger.getEmail());
            ps.setString(4, passenger.getPhone());
            ps.setDate(5, Date.valueOf(passenger.getBirthDate()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    passenger.setId(rs.getLong("id"));
                }
            }
            return passenger;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка сохранения пассажира: " + e.getMessage(), e);
        }
    }

    //  UPDATE

    @Override
    public void update(Passenger passenger) {
        String sql = """
                UPDATE passengers
                   SET full_name = ?,
                       passport_number = ?,
                       email = ?,
                       phone = ?,
                       birth_date = ?
                 WHERE id = ?
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passenger.getFullName());
            ps.setString(2, passenger.getPassportNumber());
            ps.setString(3, passenger.getEmail());
            ps.setString(4, passenger.getPhone());
            ps.setDate(5, Date.valueOf(passenger.getBirthDate()));
            ps.setLong(6, passenger.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException(
                        "Пассажир с ID=" + passenger.getId() + " не найден для обновления", null);
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка обновления пассажира: " + e.getMessage(), e);
        }
    }

    //  DELETE

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM passengers WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка удаления пассажира: " + e.getMessage(), e);
        }
    }

    //  READ

    @Override
    public Optional<Passenger> findById(Long id) {
        String sql = "SELECT * FROM passengers WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка поиска пассажира по ID=" + id, e);
        }
    }

    @Override
    public List<Passenger> findAll() {
        String sql = "SELECT * FROM passengers ORDER BY id";
        List<Passenger> result = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка получения списка пассажиров", e);
        }
    }

    @Override
    public List<Passenger> findByFullNameLike(String fragment) {
        String sql = """
                SELECT * FROM passengers
                 WHERE LOWER(full_name) LIKE LOWER(?)
                 ORDER BY full_name
                """;
        List<Passenger> result = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + fragment + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка поиска пассажиров по ФИО", e);
        }
    }

    @Override
    public Optional<Passenger> findByPassportNumber(String passportNumber) {
        String sql = "SELECT * FROM passengers WHERE passport_number = ?";
        return findSingle(sql, passportNumber);
    }

    @Override
    public Optional<Passenger> findByEmail(String email) {
        String sql = "SELECT * FROM passengers WHERE LOWER(email) = LOWER(?)";
        return findSingle(sql, email);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM passengers WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка проверки существования пассажира", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM passengers";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта пассажиров", e);
        }
    }

    //  Внутренние методы

    private Optional<Passenger> findSingle(String sql, String param) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка поиска пассажира: " + e.getMessage(), e);
        }
    }

    private Passenger mapRow(ResultSet rs) throws SQLException {
        return new Passenger(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("passport_number"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getObject("birth_date", LocalDate.class)
        );
    }
}