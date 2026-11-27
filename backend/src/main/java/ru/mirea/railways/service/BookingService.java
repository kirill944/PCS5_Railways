package ru.mirea.railways.service;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.*;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.error.NotFoundException;
import ru.mirea.railways.error.VersionConflictException;
import ru.mirea.railways.filter.BookingFilter;
import ru.mirea.railways.filter.BookingSpecification;
import ru.mirea.railways.repository.BookingRepository;
import ru.mirea.railways.repository.TagRepository;

import java.time.LocalDate;
import java.util.*;

@Service
public class BookingService {
    private final BookingRepository repo;
    private final TagRepository tagRepo;
    private final PassengerService passengers;
    private final AuditService audit;

    /** Поля, по которым действительно можно сортировать (иначе PropertyReferenceException -> 500). */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "departureDate", "departureTime",
            "trainNumber", "routeFrom", "routeTo", "price", "status", "externalCode");

    public BookingService(BookingRepository repo, TagRepository tagRepo,
                          PassengerService passengers, AuditService audit) {
        this.repo = repo; this.tagRepo = tagRepo;
        this.passengers = passengers; this.audit = audit;
    }

    public PageResponse<BookingResponse> search(BookingFilter filter, int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String field = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);
        Page<Booking> result = repo.findAll(BookingSpecification.from(filter), pageable);
        List<BookingResponse> content = result.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    /**
     * Чтение одной брони. Транзакция обязательна: open-in-view отключён,
     * поэтому ленивый b.getPassenger() нужно инициализировать внутри сессии,
     * иначе toDto() падает с LazyInitializationException (500).
     */
    @Transactional(readOnly = true)
    public BookingResponse get(UUID id) {
        return toDto(require(id));
    }

    @Transactional
    public BookingResponse create(BookingRequest r) {
        validateDates(r);
        Passenger p = passengers.require(r.passengerId());
        checkSeatFree(r, null);
        Booking b = new Booking();
        apply(b, r, p);
        // При создании статус по умолчанию CREATED (если не передан явно)
        BookingStatus requested = BookingStatus.parse(r.status());
        b.setStatus(requested == null ? BookingStatus.CREATED : requested);
        return toDto(repo.save(b));
    }

    @Transactional
    public BookingResponse update(UUID id, BookingRequest r) {
        Booking b = require(id);

        // 1. Проверка версии (оптимистичная блокировка)
        if (r.version() != null && r.version() != b.getVersion())
            throw new VersionConflictException("Запись изменена другим пользователем");

        // 2. Валидация дат
        validateDates(r);

        // 3. Валидация статуса (переходов)
        BookingStatus requested = BookingStatus.parse(r.status());
        if (requested != null) {
            validateStatusTransition(b.getStatus(), requested);
        }

        // 4. Проверка занятости места
        Passenger p = passengers.require(r.passengerId());
        checkSeatFree(r, id);

        // 5. Применение изменений
        apply(b, r, p);
        return toDto(repo.save(b));
    }

    @Transactional
    public void archive(UUID id) {
        Booking b = require(id);
        b.setArchived(true);
        repo.save(b);
        audit.log("BOOKING_ARCHIVED", "Booking", id, "Бронь архивирована: " + b.getExternalCode());
    }

    @Transactional
    public void restore(UUID id) {
        Booking b = require(id);
        b.setArchived(false);
        repo.save(b);
        audit.log("BOOKING_RESTORED", "Booking", id, "Бронь восстановлена: " + b.getExternalCode());
    }

    @Transactional
    public void permanentDelete(UUID id, boolean allowed) {
        if (!allowed) throw new BusinessException("Окончательное удаление запрещено");
        repo.deleteById(id);
        audit.log("BOOKING_DELETED", "Booking", id, "Бронь удалена окончательно");
    }

    @Transactional
    public BulkResult bulk(BulkRequest r) {
        List<UUID> ok = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (UUID id : r.ids()) {
            try {
                Booking b = require(id);
                switch (r.action()) {
                    case "ARCHIVE" -> b.setArchived(true);
                    case "RESTORE" -> b.setArchived(false);
                    case "STATUS" -> {
                        BookingStatus newStatus = BookingStatus.parse(r.status());
                        if (newStatus == null) {
                            throw new BusinessException("Для массовой смены статуса укажите поле status");
                        }
                        validateStatusTransition(b.getStatus(), newStatus);
                        b.setStatus(newStatus);
                    }
                    case "ADD_TAG" -> b.getTags().add(getOrCreateTag(r.tag()));
                    case "REMOVE_TAG" -> b.getTags().removeIf(t -> t.getName().equalsIgnoreCase(r.tag()));
                    default -> throw new BusinessException("Unknown action");
                }
                repo.save(b);
                ok.add(id);
            } catch (Exception e) {
                errors.add(id + ": " + e.getMessage());
            }
        }
        return new BulkResult(ok.size(), errors.size(), ok, errors);
    }

    /**
     * Бизнес-правило из КР2: Проверка допустимых переходов статусов.
     * Из CANCELLED нельзя никуда.
     * В PAID можно только из CONFIRMED.
     */
    private void validateStatusTransition(BookingStatus from, BookingStatus to) {
        if (from == to) {
            return; // Статус не изменился
        }

        if (from == BookingStatus.CANCELLED) {
            throw new BusinessException("Из статуса CANCELLED нельзя перейти в другой статус");
        }

        if (to == BookingStatus.PAID && from != BookingStatus.CONFIRMED) {
            throw new BusinessException("Оплатить можно только подтвержденную бронь (CONFIRMED)");
        }

        // Дополнительное правило из КР2: отменять можно всё, кроме уже завершенных
        if (to == BookingStatus.CANCELLED && from == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя отменить завершенную поездку");
        }
    }

    private Tag getOrCreateTag(String name) {
        return tagRepo.findByNameIgnoreCase(name).orElseGet(() -> tagRepo.save(new Tag(name)));
    }

    private void validateDates(BookingRequest r) {
        if (r.departureDate().isBefore(LocalDate.now()))
            throw new BusinessException("Дата отправления не может быть в прошлом");
    }

    private void checkSeatFree(BookingRequest r, UUID excludeId) {
        // Запрос к БД вместо перебора страниц в Java: быстро и не зависит от лимита 1000.
        boolean taken = excludeId == null
                ? repo.existsByTrainNumberAndDepartureDateAndWagonNumberAndSeatNumberAndStatusNotAndArchivedFalse(
                        r.trainNumber(), r.departureDate(), r.wagonNumber(), r.seatNumber(),
                        BookingStatus.CANCELLED)
                : repo.existsByTrainNumberAndDepartureDateAndWagonNumberAndSeatNumberAndStatusNotAndArchivedFalseAndIdNot(
                        r.trainNumber(), r.departureDate(), r.wagonNumber(), r.seatNumber(),
                        BookingStatus.CANCELLED, excludeId);
        if (taken) {
            throw new BusinessException(String.format(
                    "Место %d в вагоне %d поезда %s на %s уже занято. Выберите другое место или вагон.",
                    r.seatNumber(), r.wagonNumber(), r.trainNumber(), r.departureDate()));
        }
    }

    private Booking require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Бронь не найдена"));
    }

    private void apply(Booking b, BookingRequest r, Passenger p) {
        b.setPassenger(p);
        b.setTrainNumber(r.trainNumber());
        b.setRouteFrom(r.routeFrom());
        b.setRouteTo(r.routeTo());
        b.setDepartureDate(r.departureDate());
        b.setDepartureTime(r.departureTime());
        b.setWagonNumber(r.wagonNumber());
        b.setSeatNumber(r.seatNumber());
        b.setPrice(r.price());
        BookingStatus requested = BookingStatus.parse(r.status());
        if (requested != null) b.setStatus(requested);
        b.setPaid(r.paid());
        if (r.tags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (String t : r.tags()) tags.add(getOrCreateTag(t));
            b.setTags(tags);
        }
    }

    public BookingResponse toDto(Booking b) {
        return new BookingResponse(
                b.getId(), b.getExternalCode(),
                b.getPassenger().getId(), b.getPassenger().getFullName(),
                b.getTrainNumber(), b.getRouteFrom(), b.getRouteTo(),
                b.getDepartureDate(), b.getDepartureTime(),
                b.getWagonNumber(), b.getSeatNumber(), b.getPrice(),
                b.getStatus().name(), b.getRiskLevel().name(),
                b.isPaid(), b.isArchived(),
                b.getTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.toSet()),
                b.getVersion(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }
}