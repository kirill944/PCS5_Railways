package ru.mirea.railways.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static String email() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : String.valueOf(auth.getPrincipal());
    }
}