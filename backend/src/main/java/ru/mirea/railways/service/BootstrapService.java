package ru.mirea.railways.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mirea.railways.config.AppProperties;
import ru.mirea.railways.domain.Role;
import ru.mirea.railways.domain.UserAccount;
import ru.mirea.railways.repository.UserAccountRepository;

@Component
public class BootstrapService implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public BootstrapService(UserAccountRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users; this.encoder = encoder; this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(props.bootstrap().email())) return;
        UserAccount u = new UserAccount();
        u.setEmail(props.bootstrap().email().toLowerCase());
        u.setFullName(props.bootstrap().name());
        u.setPasswordHash(encoder.encode(props.bootstrap().password()));
        u.setRole(Role.ROLE_ADMIN);
        u.setEnabled(true);
        u.setMustChangePassword(true);
        users.save(u);
    }
}