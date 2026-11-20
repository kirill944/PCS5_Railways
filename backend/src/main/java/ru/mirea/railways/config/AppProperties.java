package ru.mirea.railways.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Bootstrap bootstrap,
        Cors cors,
        Security security
) {
    public record Jwt(String secretBase64, String issuer, long accessMinutes, long refreshDays) {}
    public record Bootstrap(String email, String name, String password) {}
    public record Cors(String allowedOrigins) {}
    public record Security(boolean cookieSecure, boolean permanentDeleteEnabled,
                           int maxFailedLoginAttempts, int lockMinutes) {}
}