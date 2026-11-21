package com.buytogether.security;

import com.buytogether.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-that-is-at-least-32-bytes";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);
    private final JwtService jwtService = new JwtService(
            new JwtProperties(SECRET, Duration.ofHours(1), "buy-together"), clock);

    @Test
    void issuedTokenCanBeVerified() {
        JwtService.IssuedToken issued = jwtService.issue(new AuthUser("user-1", "+919876543210"));

        assertThat(issued.expiresAt()).isEqualTo(Instant.parse("2026-11-21T11:00:00Z"));
        assertThat(jwtService.verify(issued.token()))
                .contains(new AuthUser("user-1", "+919876543210"));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.issue(new AuthUser("user-1", "+919876543210")).token();
        clock.advance(Duration.ofHours(2));

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService(
                new JwtProperties("another-secret-key-that-is-also-32-bytes-long", Duration.ofHours(1), "buy-together"),
                clock);
        String foreignToken = other.issue(new AuthUser("user-1", "+919876543210")).token();

        assertThat(jwtService.verify(foreignToken)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.verify("not-a-jwt")).isEmpty();
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofHours(1), "buy-together"), clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
