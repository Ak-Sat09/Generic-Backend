package com.example.userservice.token;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Long-lived refresh tokens with rotation: every refresh hands out a new token and retires the old one.
 * If a retired token comes back, it was copied by someone else, so the whole login is ended.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String SESSION_ENDED = "Your session has ended, please log in again";

    private final RefreshTokenStore store;
    private final Duration ttl;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    RefreshTokenService(RefreshTokenStore store, UserServiceProperties properties) {
        this(store, properties.jwt().refreshTokenTtl(), Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenStore store, Duration ttl, Clock clock) {
        this.store = store;
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Starts a new login. */
    public IssuedRefreshToken issue(Long userId) {
        return issue(userId, UUID.randomUUID().toString());
    }

    /** Swaps a valid refresh token for a new one in the same login. */
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = store.findByHash(hash(rawToken))
                .orElseThrow(() -> UserServiceException.unauthorized(SESSION_ENDED));

        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            throw UserServiceException.unauthorized(SESSION_ENDED);
        }
        if (current.getUsedAt() != null || !store.markUsed(current.getId(), now)) {
            log.warn("Refresh token reused for user {}: ending that login", current.getUserId());
            store.revokeFamily(current.getFamilyId(), now);
            throw UserServiceException.unauthorized(SESSION_ENDED);
        }
        return new Rotation(current.getUserId(), issue(current.getUserId(), current.getFamilyId()));
    }

    /** Logout on this device: ends the login this token belongs to. Unknown tokens are ignored. */
    public void revoke(String rawToken) {
        store.findByHash(hash(rawToken)).ifPresent(token -> store.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    /** Logout everywhere. */
    public void revokeAll(Long userId) {
        store.revokeAllForUser(userId, clock.instant());
    }

    private IssuedRefreshToken issue(Long userId, String familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        store.save(new RefreshToken(hash(raw), familyId, userId, now, expiresAt));
        return new IssuedRefreshToken(raw, expiresAt);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }

    public record Rotation(Long userId, IssuedRefreshToken refreshToken) {
    }
}
