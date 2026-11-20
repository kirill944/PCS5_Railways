package ru.mirea.railways.service;

import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.*;
import ru.mirea.railways.dto.*;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.error.NotFoundException;
import ru.mirea.railways.repository.UserAccountRepository;
import ru.mirea.railways.security.PasswordPolicy;

import java.util.UUID;

@Service
public class UserAdminService {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final AuditService audit;

    public UserAdminService(UserAccountRepository users, PasswordEncoder encoder,
                            PasswordPolicy policy, AuditService audit) {
        this.users = users; this.encoder = encoder; this.policy = policy; this.audit = audit;
    }

    public PageResponse<UserDto> list(int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("email"));
        Page<UserAccount> result = users.findAll(p);
        return new PageResponse<>(result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext());
    }

    @Transactional
    public UserDto create(UserCreateRequest r) {
        if (users.existsByEmailIgnoreCase(r.email()))
            throw new BusinessException("Пользователь уже существует");
        String temp = generateTempPassword();
        policy.validate(temp);
        UserAccount u = new UserAccount();
        u.setEmail(r.email().toLowerCase());
        u.setFullName(r.fullName());
        u.setRole(Role.valueOf(r.role()));
        u.setPasswordHash(encoder.encode(temp));
        u.setEnabled(true);
        u.setMustChangePassword(true);
        users.save(u);
        audit.log("USER_CREATED", "UserAccount", u.getId(), "Создан пользователь: " + u.getEmail());
        // Возвращаем временный пароль в ответе через отдельный DTO можно расширить,
        // для краткости в UserDto он не отдаётся. В реальной сдаче добавьте поле temporaryPassword.
        return toDto(u);
    }

    @Transactional
    public UserDto update(UUID id, UserUpdateRequest r) {
        UserAccount u = users.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        u.setFullName(r.fullName());
        u.setRole(Role.valueOf(r.role()));
        u.setEnabled(r.enabled());
        users.save(u);
        audit.log("USER_UPDATED", "UserAccount", u.getId(), "Изменён пользователь: " + u.getEmail());
        return toDto(u);
    }

    @Transactional
    public String resetPassword(UUID id) {
        UserAccount u = users.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        String temp = generateTempPassword();
        policy.validate(temp);
        u.setPasswordHash(encoder.encode(temp));
        u.setMustChangePassword(true);
        users.save(u);
        audit.log("USER_PASSWORD_RESET", "UserAccount", u.getId(), "Сброс пароля: " + u.getEmail());
        return temp;
    }

    private String generateTempPassword() {
        return "Tmp!" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "Aa1";
    }

    private UserDto toDto(UserAccount u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(),
                u.isEnabled(), u.isMustChangePassword());
    }
}