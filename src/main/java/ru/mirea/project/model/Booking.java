package ru.mirea.railway.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Бронирование железнодорожного билета — основная сущность системы.
 * Много бронирований принадлежат одному пассажиру (M:1).
 */
public class Booking {

    private Long id;
    private Long passengerId;
    private String trainNumber;
    private String routeFrom;
    private String routeTo;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private int wagonNumber;
    private int seatNumber;
    private BigDecimal price;
    private BookingStatus status;
    private LocalDateTime createdAt;

    /** Конструктор для создания новой брони (id и createdAt ещё нет). */
    public Booking(Long passengerId,
                   String trainNumber,
                   String routeFrom,
                   String routeTo,
                   LocalDate departureDate,
                   LocalTime departureTime,
                   int wagonNumber,
                   int seatNumber,
                   BigDecimal price,
                   BookingStatus status) {
        this.passengerId = passengerId;
        this.trainNumber = trainNumber;
        this.routeFrom = routeFrom;
        this.routeTo = routeTo;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.wagonNumber = wagonNumber;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
    }

    /** Полный конструктор — используется репозиторием при чтении из БД. */
    public Booking(Long id,
                   Long passengerId,
                   String trainNumber,
                   String routeFrom,
                   String routeTo,
                   LocalDate departureDate,
                   LocalTime departureTime,
                   int wagonNumber,
                   int seatNumber,
                   BigDecimal price,
                   BookingStatus status,
                   LocalDateTime createdAt) {
        this.id = id;
        this.passengerId = passengerId;
        this.trainNumber = trainNumber;
        this.routeFrom = routeFrom;
        this.routeTo = routeTo;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.wagonNumber = wagonNumber;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- Геттеры ---
    public Long getId()                  { return id; }
    public Long getPassengerId()         { return passengerId; }
    public String getTrainNumber()       { return trainNumber; }
    public String getRouteFrom()         { return routeFrom; }
    public String getRouteTo()           { return routeTo; }
    public LocalDate getDepartureDate()  { return departureDate; }
    public LocalTime getDepartureTime()  { return departureTime; }
    public int getWagonNumber()          { return wagonNumber; }
    public int getSeatNumber()           { return seatNumber; }
    public BigDecimal getPrice()         { return price; }
    public BookingStatus getStatus()     { return status; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    // --- Сеттеры ---
    public void setId(Long id)                             { this.id = id; }
    public void setPassengerId(Long passengerId)           { this.passengerId = passengerId; }
    public void setTrainNumber(String trainNumber)         { this.trainNumber = trainNumber; }
    public void setRouteFrom(String routeFrom)             { this.routeFrom = routeFrom; }
    public void setRouteTo(String routeTo)                 { this.routeTo = routeTo; }
    public void setDepartureDate(LocalDate departureDate)  { this.departureDate = departureDate; }
    public void setDepartureTime(LocalTime departureTime)  { this.departureTime = departureTime; }
    public void setWagonNumber(int wagonNumber)            { this.wagonNumber = wagonNumber; }
    public void setSeatNumber(int seatNumber)              { this.seatNumber = seatNumber; }
    public void setPrice(BigDecimal price)                 { this.price = price; }
    public void setStatus(BookingStatus status)            { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Booking{id=%d, passengerId=%d, train='%s', route='%s -> %s', " +
                        "departure=%s %s, wagon=%d, seat=%d, price=%s, status=%s, createdAt=%s}",
                id, passengerId, trainNumber, routeFrom, routeTo,
                departureDate, departureTime, wagonNumber, seatNumber,
                price, status, createdAt);
    }
}