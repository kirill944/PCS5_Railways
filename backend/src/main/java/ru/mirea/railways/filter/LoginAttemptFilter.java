package ru.mirea.railways.filter;

import jakarta.servlet.*;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Небольшой фильтр-маркер для аудита попыток логина.
 * Основная защита от брутфорса реализована в AuthService через поля
 * failedLoginAttempts / lockedUntil у UserAccount.
 */
@Component
public class LoginAttemptFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);
    }
}