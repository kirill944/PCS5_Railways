package ru.mirea.railways.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.railways.domain.BookingNote;
import java.util.List;
import java.util.UUID;

public interface BookingNoteRepository extends JpaRepository<BookingNote, UUID> {
    List<BookingNote> findByBookingIdOrderByPinnedDescCreatedAtDesc(UUID bookingId);
}