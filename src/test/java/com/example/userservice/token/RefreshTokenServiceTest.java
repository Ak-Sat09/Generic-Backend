package com.example.userservice.token;

import com.example.userservice.common.UserServiceException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceTest {

    private static final Instant START = Instant.parse("2026-11-28T10:00:00Z");

    private final InMemoryStore store = new InMemoryStore();
    private final MutableClock clock = new MutableClock(START);
    private final RefreshTokenService service = new RefreshTokenService(store, Duration.ofDays(7), clock);

    @Test
    void storesOnlyAHash() {
        String raw = service.issue(7L).token();

        assertThat(store.tokens.values()).allSatisfy(token -> assertThat(token.getTokenHash()).isNotEqualTo(raw));
        assertThat(store.findByHash(RefreshTokenService.hash(raw))).isPresent();
    }

    @Test
    void rotationGivesANewTokenAndRetiresTheOld() {
        String first = service.issue(7L).token();

        RefreshTokenService.Rotation rotation = service.rotate(first);

        assertThat(rotation.userId()).isEqualTo(7L);
        assertThat(rotation.refreshToken().token()).isNotEqualTo(first);
        assertThat(service.rotate(rotation.refreshToken().token()).userId()).isEqualTo(7L);
    }

    @Test
    void reusingAnOldTokenEndsTheWholeLogin() {
        String first = service.issue(7L).token();
        String second = service.rotate(first).refreshToken().token();

        assertThatThrownBy(() -> service.rotate(first)).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> service.rotate(second)).isInstanceOf(UserServiceException.class);
    }

    @Test
    void expiredTokensAreRefused() {
        String raw = service.issue(7L).token();
        clock.now = START.plus(Duration.ofDays(8));

        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UserServiceException.class);
    }

    @Test
    void logoutEndsOnlyThatLogin() {
        String phone = service.issue(7L).token();
        String laptop = service.issue(7L).token();

        service.revoke(phone);

        assertThatThrownBy(() -> service.rotate(phone)).isInstanceOf(UserServiceException.class);
        assertThat(service.rotate(laptop).userId()).isEqualTo(7L);
    }

    @Test
    void logoutEverywhereEndsEveryLogin() {
        String phone = service.issue(7L).token();
        String laptop = service.issue(7L).token();
        String someoneElse = service.issue(8L).token();

        service.revokeAll(7L);

        assertThatThrownBy(() -> service.rotate(phone)).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> service.rotate(laptop)).isInstanceOf(UserServiceException.class);
        assertThat(service.rotate(someoneElse).userId()).isEqualTo(8L);
    }

    @Test
    void unknownTokensAreRefused() {
        assertThatThrownBy(() -> service.rotate("made-up")).isInstanceOf(UserServiceException.class);
    }

    static final class InMemoryStore implements RefreshTokenStore {
        final Map<String, RefreshToken> tokens = new HashMap<>();

        @Override
        public void save(RefreshToken token) {
            token.setId(UUID.randomUUID().toString());
            tokens.put(token.getId(), token);
        }

        @Override
        public Optional<RefreshToken> findByHash(String tokenHash) {
            return tokens.values().stream().filter(t -> t.getTokenHash().equals(tokenHash)).findFirst();
        }

        @Override
        public boolean markUsed(String id, Instant at) {
            RefreshToken token = tokens.get(id);
            if (token.getUsedAt() != null || token.getRevokedAt() != null) {
                return false;
            }
            token.setUsedAt(at);
            return true;
        }

        @Override
        public void revokeFamily(String familyId, Instant at) {
            tokens.values().stream().filter(t -> t.getFamilyId().equals(familyId)).forEach(t -> t.setRevokedAt(at));
        }

        @Override
        public void revokeAllForUser(Long userId, Instant at) {
            tokens.values().stream().filter(t -> t.getUserId().equals(userId)).forEach(t -> t.setRevokedAt(at));
        }
    }

    static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
