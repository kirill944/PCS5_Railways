package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.BookingNote;
import ru.mirea.railways.dto.NoteDto;
import ru.mirea.railways.dto.NoteRequest;
import ru.mirea.railways.error.NotFoundException;
import ru.mirea.railways.repository.BookingNoteRepository;
import ru.mirea.railways.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {
    private final BookingNoteRepository repo;
    public NoteService(BookingNoteRepository repo) { this.repo = repo; }

    public List<NoteDto> list(UUID bookingId) {
        return repo.findByBookingIdOrderByPinnedDescCreatedAtDesc(bookingId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public NoteDto create(UUID bookingId, NoteRequest r) {
        BookingNote n = new BookingNote();
        n.setBookingId(bookingId);
        n.setAuthorEmail(CurrentUser.email());
        n.setBody(r.body());
        n.setPinned(r.pinned());
        return toDto(repo.save(n));
    }

    @Transactional
    public NoteDto update(UUID noteId, NoteRequest r) {
        BookingNote n = repo.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        n.setBody(r.body());
        n.setPinned(r.pinned());
        return toDto(repo.save(n));
    }

    @Transactional
    public void delete(UUID noteId) {
        repo.deleteById(noteId);
    }

    private NoteDto toDto(BookingNote n) {
        return new NoteDto(n.getId(), n.getBookingId(), n.getAuthorEmail(),
                n.getBody(), n.isPinned(), n.getCreatedAt(), n.getUpdatedAt());
    }
}