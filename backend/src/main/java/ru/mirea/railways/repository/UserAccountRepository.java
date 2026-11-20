package ru.mirea.railways.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.railways.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}