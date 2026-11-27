package ru.mirea.railways.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.railways.domain.AuditEvent;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    /** Фильтр журнала аудита по типу события (параметр eventType игнорировался). */
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);
}