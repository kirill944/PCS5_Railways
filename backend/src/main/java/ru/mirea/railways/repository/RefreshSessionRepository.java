package ru.mirea.railways.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.railways.domain.RefreshSession;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
}