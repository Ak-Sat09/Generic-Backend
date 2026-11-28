package com.example.userservice.token;

import com.example.userservice.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * The original HS256 token from POST /api/users/login, kept until the blog and coupon frontends
 * move to the new login. The secret comes from {@code app.jwt.secret} (JWT_SECRET).
 */
@Component
public class LegacyTokenService {

    private final SecretKey key;
    private final long expirationMs;

    public LegacyTokenService(@Value("${app.jwt.secret}") String secret,
                              @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String issue(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(AccessTokenService.USER_ID_CLAIM, user.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<Long> verify(String token) {
        try {
            Object claim = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()
                    .get(AccessTokenService.USER_ID_CLAIM);
            return Optional.ofNullable(AccessTokenService.userId(claim));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
