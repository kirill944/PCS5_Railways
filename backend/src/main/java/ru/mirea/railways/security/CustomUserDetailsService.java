package ru.mirea.railways.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.mirea.railways.domain.UserAccount;
import ru.mirea.railways.repository.UserAccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;
    public CustomUserDetailsService(UserAccountRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount u = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.builder()
                .username(u.getEmail())
                .password(u.getPasswordHash())
                .authorities(u.getRole().name())
                .disabled(!u.isEnabled())
                .accountLocked(u.getLockedUntil() != null && u.getLockedUntil().isAfter(java.time.Instant.now()))
                .build();
    }
}