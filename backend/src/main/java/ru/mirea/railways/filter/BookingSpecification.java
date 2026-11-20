package ru.mirea.railways.filter;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.railways.domain.Booking;
import ru.mirea.railways.domain.Tag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    public static Specification<Booking> from(BookingFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (f.hasSearch()) {
                String like = "%" + f.search().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("trainNumber")), like),
                        cb.like(cb.lower(root.get("routeFrom")), like),
                        cb.like(cb.lower(root.get("routeTo")), like),
                        cb.like(cb.lower(root.get("externalCode")), like)
                ));
            }
            if (f.passengerId() != null) {
                ps.add(cb.equal(root.get("passenger").get("id"), f.passengerId()));
            }
            if (f.status() != null) ps.add(cb.equal(root.get("status"), f.status()));
            if (f.paid() != null) ps.add(cb.equal(root.get("paid"), f.paid()));
            if (f.archived() != null) ps.add(cb.equal(root.get("archived"), f.archived()));
            if (f.tag() != null && !f.tag().isBlank()) {
                Join<Booking, Tag> tags = root.join("tags", JoinType.LEFT);
                ps.add(cb.equal(cb.lower(tags.get("name")), f.tag().toLowerCase()));
                query.distinct(true);
            }
            if (f.priceValue() != null && f.priceOp() != null) {
                switch (f.priceOp()) {
                    case "gt" -> ps.add(cb.greaterThan(root.get("price"), f.priceValue()));
                    case "lt" -> ps.add(cb.lessThan(root.get("price"), f.priceValue()));
                    case "eq" -> ps.add(cb.equal(root.get("price"), f.priceValue()));
                    default -> {}
                }
            }
            if (f.riskLevel() != null) {
                // risk вычисляется в Java; эмулируем через комбинацию status и departureDate
                LocalDate today = LocalDate.now();
                switch (f.riskLevel()) {
                    case LOW -> ps.add(cb.or(
                            cb.equal(root.get("status"), ru.mirea.railways.domain.BookingStatus.PAID),
                            cb.equal(root.get("status"), ru.mirea.railways.domain.BookingStatus.COMPLETED),
                            cb.greaterThan(root.get("departureDate"), today.plusDays(3))
                    ));
                    case HIGH -> ps.add(cb.and(
                            cb.equal(root.get("status"), ru.mirea.railways.domain.BookingStatus.CREATED),
                            cb.lessThan(root.get("departureDate"), today.plusDays(1))
                    ));
                    case MEDIUM -> { /* оставляем общий набор, уточнять не будем */ }
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}