package ru.mirea.railways.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String externalCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(nullable = false, length = 10)
    private String trainNumber;

    @Column(nullable = false, length = 100)
    private String routeFrom;

    @Column(nullable = false, length = 100)
    private String routeTo;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private int wagonNumber;

    @Column(nullable = false)
    private int seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CREATED;

    @Column(nullable = false)
    private boolean paid = false;

    @Column(nullable = false)
    private boolean archived = false;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "booking_tag",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (externalCode == null) externalCode = "BK-" + id.toString().substring(0, 8).toUpperCase();
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    /**
     * Вычисляемый показатель: риск отмены/проблемы с бронью.
     * LOW  — статус PAID/COMPLETED или до отправления > 3 дней
     * HIGH — статус CREATED и до отправления < 1 дня
     * MEDIUM — остальные
     */
    @Transient
    public BookingRiskLevel getRiskLevel() {
        if (status == BookingStatus.COMPLETED || status == BookingStatus.PAID) return BookingRiskLevel.LOW;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), departureDate);
        if (days < 1 && status == BookingStatus.CREATED) return BookingRiskLevel.HIGH;
        if (days > 3) return BookingRiskLevel.LOW;
        return BookingRiskLevel.MEDIUM;
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalCode() { return externalCode; }
    public void setExternalCode(String externalCode) { this.externalCode = externalCode; }
    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getRouteFrom() { return routeFrom; }
    public void setRouteFrom(String routeFrom) { this.routeFrom = routeFrom; }
    public String getRouteTo() { return routeTo; }
    public void setRouteTo(String routeTo) { this.routeTo = routeTo; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public int getWagonNumber() { return wagonNumber; }
    public void setWagonNumber(int wagonNumber) { this.wagonNumber = wagonNumber; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
}