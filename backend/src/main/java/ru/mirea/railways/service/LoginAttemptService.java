package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.config.AppProperties;
import ru.mirea.railways.domain.UserAccount;
import ru.mirea.railways.repository.UserAccountRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Учёт неудачных попыток входа.
 *
 * Вынесено в отдельный бин намеренно: метод login() бросает BusinessException
 * (RuntimeException), из-за чего его транзакция откатывается и инкремент
 * счётчика терялся — блокировка никогда не срабатывала. Здесь запись
 * выполняется в ОТДЕЛЬНОЙ транзакции (REQUIRES_NEW), которая коммитится
 * независимо от исключения в вызывающем коде.
 */
@Service
public class LoginAttemptService {

    private final UserAccountRepository users;
    private final AppProperties props;

    public LoginAttemptService(UserAccountRepository users, AppProperties props) {
        this.users = users;
        this.props = props;
    }

    /**
     * Регистрирует неудачную попытку и блокирует учётку при достижении лимита.
     *
     * @return true, если учётная запись была заблокирована этой попыткой
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(UserAccount account) {
        UserAccount u = users.findById(account.getId()).orElse(account);
        u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);

        boolean locked = false;
        if (u.getFailedLoginAttempts() >= props.security().maxFailedLoginAttempts()) {
            u.setLockedUntil(Instant.now().plus(props.security().lockMinutes(), ChronoUnit.MINUTES));
            u.setFailedLoginAttempts(0);
            locked = true;
        }
        users.save(u);
        return locked;
    }

    /** Сбрасывает счётчик после успешного входа. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UserAccount account) {
        UserAccount u = users.findById(account.getId()).orElse(account);
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
    }
}