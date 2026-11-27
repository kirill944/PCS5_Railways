package ru.mirea.railways.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.railways.domain.Booking;
import ru.mirea.railways.domain.BookingStatus;

import java.time.LocalDate;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"passenger", "tags"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    /** Поиск по внешнему коду — нужен для режима UPDATE при импорте CSV. */
    java.util.Optional<Booking> findByExternalCodeIgnoreCase(String externalCode);

    /**
     * Занято ли место (отменённые и архивные брони место не занимают).
     */
    boolean existsByTrainNumberAndDepartureDateAndWagonNumberAndSeatNumberAndStatusNotAndArchivedFalse(
            String trainNumber, LocalDate departureDate, int wagonNumber, int seatNumber,
            BookingStatus status);

    /** То же, но при редактировании брони сама бронь не считается занятой. */
    boolean existsByTrainNumberAndDepartureDateAndWagonNumberAndSeatNumberAndStatusNotAndArchivedFalseAndIdNot(
            String trainNumber, LocalDate departureDate, int wagonNumber, int seatNumber,
            BookingStatus status, UUID id);
}