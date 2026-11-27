package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.AuditEvent;
import ru.mirea.railways.filter.CorrelationIdFilter;
import ru.mirea.railways.repository.AuditEventRepository;
import ru.mirea.railways.security.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    @Transactional
    public void log(String eventType, String entityType, UUID entityId, String description) {
        save(CurrentUser.email(), eventType, entityType, entityId, description);
    }

    /**
     * Запись события, которое происходит внутри операции, завершающейся
     * исключением (например, блокировка учётной записи в login()).
     * REQUIRES_NEW гарантирует коммит независимо от отката вызывающей транзакции,
     * иначе событие LOGIN_BLOCKED терялось.
     * Автор передаётся явно: на момент входа аутентификация ещё не установлена,
     * и CurrentUser.email() вернул бы anonymousUser.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAs(String actorEmail, String eventType, String entityType,
                      UUID entityId, String description) {
        save(actorEmail, eventType, entityType, entityId, description);
    }

    private void save(String actorEmail, String eventType, String entityType,
                      UUID entityId, String description) {
        AuditEvent e = new AuditEvent();
        e.setActorEmail(actorEmail);
        e.setEventType(eventType);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDescription(description);
        e.setCorrelationId(currentCorrelationId());
        repo.save(e);
    }

    private String currentCorrelationId() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            Object v = req.getAttribute(CorrelationIdFilter.ATTR);
            return v == null ? null : v.toString();
        } catch (Exception e) { return null; }
    }
}