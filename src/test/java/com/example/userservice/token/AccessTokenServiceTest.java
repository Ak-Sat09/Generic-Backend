package com.example.userservice.token;

import com.example.userservice.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");
    private static final SigningKeys KEYS = new SigningKeys(null, true);
    private static final String LEGACY_SECRET = "legacy-secret-that-is-long-enough-for-hs256-0123456789";

    private final User user = new User(1732000000000123L, "Anmol", "anmol@example.com", "hash", NOW);

    private AccessTokenService at(Instant now) {
        return new AccessTokenService(KEYS, "chronicle-user-service", Duration.ofMinutes(15), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void issuedTokenCarriesTheNumericUserId() {
        AccessTokenService.IssuedToken issued = at(NOW).issue(user);

        assertThat(issued.expiresInSeconds()).isEqualTo(900);
        assertThat(at(NOW.plusSeconds(60)).verify(issued.token())).contains(1732000000000123L);
    }

    @Test
    void expiresAfterFifteenMinutes() {
        String token = at(NOW).issue(user).token();

        assertThat(at(NOW.plus(Duration.ofMinutes(16))).verify(token)).isEmpty();
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        AccessTokenService other = new AccessTokenService(new SigningKeys(null, true), "chronicle-user-service",
                Duration.ofMinutes(15), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(at(NOW).verify(other.issue(user).token())).isEmpty();
    }

    @Test
    void rejectsAnotherIssuer() {
        AccessTokenService other = new AccessTokenService(KEYS, "someone-else", Duration.ofMinutes(15), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(at(NOW).verify(other.issue(user).token())).isEmpty();
    }

    @Test
    void verifierAcceptsNewAndOldTokensWithOrWithoutBearer() {
        LegacyTokenService legacy = new LegacyTokenService(LEGACY_SECRET, 86_400_000);
        AccessTokenVerifier verifier = new AccessTokenVerifier(at(Instant.now()), legacy);
        String fresh = at(Instant.now()).issue(user).token();

        assertThat(verifier.userId("Bearer " + fresh)).contains(user.getId());
        assertThat(verifier.userId(legacy.issue(user))).contains(user.getId());
        assertThat(verifier.userId("Bearer garbage")).isEmpty();
        assertThat(verifier.userId(null)).isEmpty();
    }
}
