package ru.mirea.railways.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.railways.domain.Passenger;
import java.util.UUID;

public interface PassengerRepository extends JpaRepository<Passenger, UUID> {
    boolean existsByPassportNumber(String passportNumber);
    boolean existsByEmail(String email);
}