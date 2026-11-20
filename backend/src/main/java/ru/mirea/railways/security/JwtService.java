package ru.mirea.railways.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.mirea.railways.config.AppProperties;
import ru.mirea.railways.domain.UserAccount;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long accessSeconds;

    public JwtService(AppProperties props) {
        byte[] secret = java.util.Base64.getDecoder().decode(props.jwt().secretBase64());
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = props.jwt().issuer();
        this.accessSeconds = props.jwt().accessMinutes() * 60;
    }

    public String issueAccessToken(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .claims(Map.of(
                        "role", user.getRole().name(),
                        "uid", user.getId().toString(),
                        "mcp", user.isMustChangePassword()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "." + UUID.randomUUID();
    }

    public String sha256(String value) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public long getAccessSeconds() { return accessSeconds; }
}