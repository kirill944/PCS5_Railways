package ru.mirea.railway.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Пассажир — участник предметной области.
 * Один пассажир может иметь много бронирований (связь 1:M).
 */
public class Passenger {

    private Long id;
    private String fullName;
    private String passportNumber;
    private String email;
    private String phone;
    private LocalDate birthDate;

    /** Конструктор для создания нового пассажира (id ещё нет). */
    public Passenger(String fullName,
                     String passportNumber,
                     String email,
                     String phone,
                     LocalDate birthDate) {
        this.fullName = fullName;
        this.passportNumber = passportNumber;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    /** Полный конструктор — используется репозиторием при чтении из БД. */
    public Passenger(Long id,
                     String fullName,
                     String passportNumber,
                     String email,
                     String phone,
                     LocalDate birthDate) {
        this.id = id;
        this.fullName = fullName;
        this.passportNumber = passportNumber;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    // --- Геттеры ---
    public Long getId()               { return id; }
    public String getFullName()       { return fullName; }
    public String getPassportNumber() { return passportNumber; }
    public String getEmail()          { return email; }
    public String getPhone()          { return phone; }
    public LocalDate getBirthDate()   { return birthDate; }

    // --- Сеттеры ---
    public void setId(Long id)                       { this.id = id; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public void setEmail(String email)               { this.email = email; }
    public void setPhone(String phone)               { this.phone = phone; }
    public void setBirthDate(LocalDate birthDate)    { this.birthDate = birthDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passenger passenger = (Passenger) o;
        return Objects.equals(id, passenger.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Passenger{id=%d, fullName='%s', passport='%s', email='%s', phone='%s', birthDate=%s}",
                id, fullName, passportNumber, email, phone, birthDate);
    }
}