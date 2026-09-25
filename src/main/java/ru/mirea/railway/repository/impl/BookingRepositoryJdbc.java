package ru.mirea.railway.repository.impl;

import ru.mirea.railway.exception.DatabaseException;
import ru.mirea.railway.model.Booking;
import ru.mirea.railway.model.BookingStatus;
import ru.mirea.railway.repository.BookingRepository;
import ru.mirea.railway.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация BookingRepository.
 * Все запросы параметризованы (PreparedStatement) — защита от SQL-инъекций.
 * Соединения и ResultSet закрываются через try-with-resources.
 */
public class BookingRepositoryJdbc implements BookingRepository {

    private final DatabaseManager db;

    /** Базовый SELECT с JOIN для получения данных пассажира, если нужно. */
    private static final String BASE_SELECT = """
            SELECT b.id, b.passenger_id, b.train_number, b.route_from, b.route_to,
                   b.departure_date, b.departure_time, b.wagon_number, b.seat_number,
                   b.price, b.status, b.created_at
              FROM bookings b
            """;

    public BookingRepositoryJdbc() {
        this.db = DatabaseManager.getInstance();
    }

    //  CREATE

    @Override
    public Booking save(Booking booking) {
        String sql = """
                INSERT INTO bookings
                    (passenger_id, train_number, route_from, route_to,
                     departure_date, departure_time, wagon_number, seat_number,
                     price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, booking.getPassengerId());
            ps.setString(2, booking.getTrainNumber());
            ps.setString(3, booking.getRouteFrom());
            ps.setString(4, booking.getRouteTo());
            ps.setDate(5, Date.valueOf(booking.getDepartureDate()));
            ps.setTime(6, Time.valueOf(booking.getDepartureTime()));
            ps.setInt(7, booking.getWagonNumber());
            ps.setInt(8, booking.getSeatNumber());
            ps.setBigDecimal(9, booking.getPrice());
            ps.setString(10, booking.getStatus().name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    booking.setId(rs.getLong("id"));
                    booking.setCreatedAt(
                            rs.getObject("created_at", LocalDateTime.class));
                }
            }
            return booking;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка сохранения брони: " + e.getMessage(), e);
        }
    }

    //  UPDATE

    @Override
    public void update(Booking booking) {
        String sql = """
                UPDATE bookings
                   SET passenger_id = ?,
                       train_number = ?,
                       route_from = ?,
                       route_to = ?,
                       departure_date = ?,
                       departure_time = ?,
                       wagon_number = ?,
                       seat_number = ?,
                       price = ?,
                       status = ?
                 WHERE id = ?
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, booking.getPassengerId());
            ps.setString(2, booking.getTrainNumber());
            ps.setString(3, booking.getRouteFrom());
            ps.setString(4, booking.getRouteTo());
            ps.setDate(5, Date.valueOf(booking.getDepartureDate()));
            ps.setTime(6, Time.valueOf(booking.getDepartureTime()));
            ps.setInt(7, booking.getWagonNumber());
            ps.setInt(8, booking.getSeatNumber());
            ps.setBigDecimal(9, booking.getPrice());
            ps.setString(10, booking.getStatus().name());
            ps.setLong(11, booking.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException(
                        "Бронирование ID=" + booking.getId() + " не найдено", null);
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка обновления брони: " + e.getMessage(), e);
        }
    }

    //  DELETE

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM bookings WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка удаления брони: " + e.getMessage(), e);
        }
    }

    //  READ

    @Override
    public Optional<Booking> findById(Long id) {
        String sql = BASE_SELECT + " WHERE b.id = ?";
        return findSingle(sql, ps -> ps.setLong(1, id));
    }

    @Override
    public List<Booking> findAll() {
        String sql = BASE_SELECT + " ORDER BY b.id";
        return findList(sql, ps -> {});
    }

    //  ПОИСК

    @Override
    public List<Booking> findByTrainNumber(String trainNumber) {
        String sql = BASE_SELECT +
                " WHERE LOWER(b.train_number) LIKE LOWER(?) ORDER BY b.departure_date";
        return findList(sql, ps -> ps.setString(1, "%" + trainNumber + "%"));
    }

    @Override
    public List<Booking> findByRoute(String from, String to) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        if (from != null && !from.isBlank()) {
            sql.append(" AND LOWER(b.route_from) LIKE LOWER(?)");
        }
        if (to != null && !to.isBlank()) {
            sql.append(" AND LOWER(b.route_to) LIKE LOWER(?)");
        }
        sql.append(" ORDER BY b.departure_date");

        return findList(sql.toString(), ps -> {
            int idx = 1;
            if (from != null && !from.isBlank()) {
                ps.setString(idx++, "%" + from + "%");
            }
            if (to != null && !to.isBlank()) {
                ps.setString(idx, "%" + to + "%");
            }
        });
    }

    @Override
    public List<Booking> findByPassengerFullName(String fragment) {
        String sql = """
                SELECT b.* FROM bookings b
                  JOIN passengers p ON p.id = b.passenger_id
                 WHERE LOWER(p.full_name) LIKE LOWER(?)
                 ORDER BY b.departure_date
                """;
        return findList(sql, ps -> ps.setString(1, "%" + fragment + "%"));
    }

    @Override
    public List<Booking> findByPassengerPassport(String passportNumber) {
        String sql = """
                SELECT b.* FROM bookings b
                  JOIN passengers p ON p.id = b.passenger_id
                 WHERE p.passport_number = ?
                 ORDER BY b.departure_date
                """;
        return findList(sql, ps -> ps.setString(1, passportNumber));
    }

    //  ФИЛЬТРАЦИЯ

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        String sql = BASE_SELECT + " WHERE b.status = ? ORDER BY b.departure_date";
        return findList(sql, ps -> ps.setString(1, status.name()));
    }

    @Override
    public List<Booking> findByPassengerId(Long passengerId) {
        String sql = BASE_SELECT + " WHERE b.passenger_id = ? ORDER BY b.departure_date";
        return findList(sql, ps -> ps.setLong(1, passengerId));
    }

    @Override
    public List<Booking> findByDepartureDateBetween(LocalDate from, LocalDate to) {
        String sql = BASE_SELECT +
                " WHERE b.departure_date BETWEEN ? AND ? ORDER BY b.departure_date";
        return findList(sql, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    //  ПРОВЕРКИ

    @Override
    public boolean isSeatTaken(String trainNumber, int wagonNumber,
                               int seatNumber, LocalDate departureDate) {
        String sql = """
                SELECT 1 FROM bookings
                 WHERE train_number = ?
                   AND wagon_number = ?
                   AND seat_number = ?
                   AND departure_date = ?
                   AND status <> 'CANCELLED'
                 LIMIT 1
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, trainNumber);
            ps.setInt(2, wagonNumber);
            ps.setInt(3, seatNumber);
            ps.setDate(4, Date.valueOf(departureDate));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка проверки занятости места", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM bookings WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки брони", e);
        }
    }

    //  СТАТИСТИКА

    @Override
    public long count() {
        return countBySql("SELECT COUNT(*) FROM bookings", ps -> {});
    }

    @Override
    public long countByStatus(BookingStatus status) {
        return countBySql("SELECT COUNT(*) FROM bookings WHERE status = ?",
                ps -> ps.setString(1, status.name()));
    }

    //  Внутренние методы

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private Optional<Booking> findSingle(String sql, StatementSetter setter) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка поиска брони: " + e.getMessage(), e);
        }
    }

    private List<Booking> findList(String sql, StatementSetter setter) {
        List<Booking> result = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Ошибка получения списка броней: " + e.getMessage(), e);
        }
    }

    private long countBySql(String sql, StatementSetter setter) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подсчёта", e);
        }
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getLong("id"),
                rs.getLong("passenger_id"),
                rs.getString("train_number"),
                rs.getString("route_from"),
                rs.getString("route_to"),
                rs.getObject("departure_date", LocalDate.class),
                rs.getObject("departure_time", LocalTime.class),
                rs.getInt("wagon_number"),
                rs.getInt("seat_number"),
                rs.getBigDecimal("price"),
                BookingStatus.fromString(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}