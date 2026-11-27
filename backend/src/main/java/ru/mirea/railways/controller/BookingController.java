package ru.mirea.railways.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mirea.railways.config.AppProperties;
import ru.mirea.railways.domain.BookingRiskLevel;
import ru.mirea.railways.domain.BookingStatus;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.filter.BookingFilter;
import ru.mirea.railways.service.BookingService;
import ru.mirea.railways.service.CsvService;
import ru.mirea.railways.service.NoteService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    private final NoteService notes;
    private final CsvService csv;
    private final AppProperties props;

    public BookingController(BookingService service, NoteService notes, CsvService csv, AppProperties props) {
        this.service = service; this.notes = notes; this.csv = csv; this.props = props;
    }

    @GetMapping
    public PageResponse<BookingResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID passengerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String priceOp,
            @RequestParam(required = false) BigDecimal priceValue,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BookingFilter f = new BookingFilter(search, passengerId,
                BookingStatus.parse(status),
                BookingRiskLevel.parse(riskLevel),
                paid, archived, tag, priceOp, priceValue, sortBy, sortDir);
        return service.search(f, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable UUID id, @Valid @RequestBody BookingRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archive(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        service.restore(id); return ResponseEntity.noContent().build();
    }

    /**
     * Окончательное (безвозвратное) удаление — требование КР3 п. 3.6:
     * только администратор. До этого метода проверялась лишь роль EDITOR/ADMIN
     * из SecurityConfig, то есть редактор тоже мог удалять при включённом флаге.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanent(@PathVariable UUID id) {
        service.permanentDelete(id, props.security().permanentDeleteEnabled());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@Valid @RequestBody BulkRequest r) { return service.bulk(r); }

    @PostMapping("/import/preview")
    public ImportPreviewResponse preview(@Valid @RequestBody ImportPreviewRequest r) {
        return csv.preview(r);
    }

    @PostMapping("/import/apply")
    public ImportPreviewResponse apply(@Valid @RequestBody ImportApplyRequest r) {
        return csv.apply(r);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(defaultValue = "1000") int max
    ) {
        BookingFilter f = new BookingFilter(search, null,
                BookingStatus.parse(status),
                null, null, archived, null, null, null, "createdAt", "desc");
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=bookings.csv")
                .body(csv.exportCsv(f, max));
    }

    @GetMapping("/{id}/notes")
    public List<NoteDto> notes(@PathVariable UUID id) { return notes.list(id); }

    @PostMapping("/{id}/notes")
    public ResponseEntity<NoteDto> addNote(@PathVariable UUID id, @Valid @RequestBody NoteRequest r) {
        return ResponseEntity.status(201).body(notes.create(id, r));
    }

    @PutMapping("/notes/{noteId}")
    public NoteDto updateNote(@PathVariable UUID noteId, @Valid @RequestBody NoteRequest r) {
        return notes.update(noteId, r);
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID noteId) {
        notes.delete(noteId); return ResponseEntity.noContent().build();
    }
}