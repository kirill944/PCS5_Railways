package ru.mirea.railways.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.config.AppProperties;
import ru.mirea.railways.domain.RefreshSession;
import ru.mirea.railways.domain.UserAccount;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.repository.RefreshSessionRepository;
import ru.mirea.railways.repository.UserAccountRepository;
import ru.mirea.railways.security.JwtService;
import ru.mirea.railways.security.PasswordPolicy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PasswordPolicy policy;
    private final AppProperties props;
    private final AuditService audit;

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       PasswordEncoder encoder, JwtService jwt, PasswordPolicy policy,
                       AppProperties props, AuditService audit) {
        this.users = users; this.sessions = sessions; this.encoder = encoder;
        this.jwt = jwt; this.policy = policy; this.props = props; this.audit = audit;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        UserAccount u = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BusinessException("Неверный логин или пароль"));
        if (!u.isEnabled()) throw new BusinessException("Учётная запись отключена");
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now()))
            throw new BusinessException("Учётная запись временно заблокирована");
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            if (u.getFailedLoginAttempts() >= props.security().maxFailedLoginAttempts()) {
                u.setLockedUntil(Instant.now().plus(props.security().lockMinutes(), ChronoUnit.MINUTES));
                u.setFailedLoginAttempts(0);
            }
            users.save(u);
            throw new BusinessException("Неверный логин или пароль");
        }
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        users.save(u);

        String access = jwt.issueAccessToken(u);
        String refresh = issueRefresh(u.getId());
        audit.log("LOGIN", "UserAccount", u.getId(), "Успешный вход: " + u.getEmail());
        return new LoginResponse(access, refresh, jwt.getAccessSeconds(),
                u.isMustChangePassword(), toDto(u));
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest req) {
        String hash = jwt.sha256(req.refreshToken());
        RefreshSession s = sessions.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
        if (s.isRevoked() || s.getExpiresAt().isBefore(Instant.now()))
            throw new BusinessException("Refresh token expired or revoked");
        s.setRevoked(true);
        sessions.save(s);
        UserAccount u = users.findById(s.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        String access = jwt.issueAccessToken(u);
        String newRefresh = issueRefresh(u.getId());
        return new LoginResponse(access, newRefresh, jwt.getAccessSeconds(),
                u.isMustChangePassword(), toDto(u));
    }

    @Transactional
    public void logout(RefreshRequest req) {
        sessions.findByTokenHash(jwt.sha256(req.refreshToken())).ifPresent(s -> {
            s.setRevoked(true);
            sessions.save(s);
        });
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        UserAccount u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash()))
            throw new BusinessException("Текущий пароль неверен");
        policy.validate(req.newPassword());
        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setMustChangePassword(false);
        users.save(u);
        audit.log("PASSWORD_CHANGED", "UserAccount", u.getId(), "Пароль изменён: " + email);
    }

    public UserDto me(String email) {
        return users.findByEmailIgnoreCase(email).map(this::toDto)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private String issueRefresh(java.util.UUID userId) {
        String raw = jwt.generateRefreshToken();
        RefreshSession s = new RefreshSession();
        s.setUserId(userId);
        s.setTokenHash(jwt.sha256(raw));
        s.setExpiresAt(Instant.now().plus(props.jwt().refreshDays(), ChronoUnit.DAYS));
        sessions.save(s);
        return raw;
    }

    public UserDto toDto(UserAccount u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(),
                u.isEnabled(), u.isMustChangePassword());
    }
}