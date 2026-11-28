package com.example.userservice.auth;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.token.AccessTokenService;
import com.example.userservice.token.RefreshTokenService;
import com.example.userservice.user.User;
import com.example.userservice.user.UserIdSequence;
import com.example.userservice.user.UserRepository;
import com.example.userservice.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");
    private static final String PASSWORD = "correct-horse-battery";

    private final UserRepository users = mock(UserRepository.class);
    private final UserIdSequence ids = mock(UserIdSequence.class);
    private final PasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private final AccessTokenService accessTokens = mock(AccessTokenService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private UserAuthService auth;
    private User user;

    @BeforeEach
    void setUp() {
        auth = new UserAuthService(users, ids, passwords, accessTokens, refreshTokens, 5, Duration.ofMinutes(15),
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User(42L, "Anmol", "anmol@example.com", passwords.encode(PASSWORD), NOW.minusSeconds(3600));
        when(users.findByEmail("anmol@example.com")).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void rightPasswordLogsInAndResetsFailures() {
        user.setFailedLogins(3);

        User loggedIn = auth.authenticate(" Anmol@Example.com ", PASSWORD);

        assertThat(loggedIn.failedLoginCount()).isZero();
        assertThat(loggedIn.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void unknownEmailAndWrongPasswordLookTheSame() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertStatus(() -> auth.authenticate("nobody@example.com", PASSWORD), HttpStatus.UNAUTHORIZED, UserAuthService.INVALID_CREDENTIALS);
        assertStatus(() -> auth.authenticate("anmol@example.com", "wrong-password"), HttpStatus.UNAUTHORIZED, UserAuthService.INVALID_CREDENTIALS);
    }

    @Test
    void fiveWrongPasswordsLockTheAccount() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> auth.authenticate("anmol@example.com", "wrong-password")).isInstanceOf(UserServiceException.class);
        }

        assertThat(user.getLockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertStatus(() -> auth.authenticate("anmol@example.com", PASSWORD), HttpStatus.TOO_MANY_REQUESTS, "Try again in");
    }

    @Test
    void lockEndsAfterItsTime() {
        user.setLockedUntil(NOW.minusSeconds(1));

        assertThat(auth.authenticate("anmol@example.com", PASSWORD).getLockedUntil()).isNull();
    }

    @Test
    void disabledAccountsCannotLogIn() {
        user.setStatus(UserStatus.DISABLED);

        assertStatus(() -> auth.authenticate("anmol@example.com", PASSWORD), HttpStatus.FORBIDDEN, "disabled");
    }

    @Test
    void accountsWithoutPasswordCannotUsePasswordLogin() {
        user.setPasswordHash(null);

        assertStatus(() -> auth.authenticate("anmol@example.com", PASSWORD), HttpStatus.UNAUTHORIZED, UserAuthService.INVALID_CREDENTIALS);
    }

    @Test
    void registerNormalisesEmailAndUsesTheSequence() {
        when(ids.next()).thenReturn(1001L);

        User created = auth.register("  Priya ", "Priya@Example.COM", PASSWORD);

        assertThat(created.getId()).isEqualTo(1001L);
        assertThat(created.getEmail()).isEqualTo("priya@example.com");
        assertThat(created.getName()).isEqualTo("Priya");
        assertThat(passwords.matches(PASSWORD, created.getPasswordHash())).isTrue();
        assertThat(created.isEmailVerified()).isFalse();
    }

    @Test
    void registerRefusesTakenEmailAndOverlongPassword() {
        when(users.existsByEmail("anmol@example.com")).thenReturn(true);

        assertStatus(() -> auth.register("Anmol", "anmol@example.com", PASSWORD), HttpStatus.CONFLICT, "already registered");
        assertStatus(() -> auth.register("Long", "long@example.com", "x".repeat(73)), HttpStatus.BAD_REQUEST, "too long");
        verify(ids, never()).next();
    }

    @Test
    void emailOtpOrGoogleReusesTheAccountAndConfirmsTheEmail() {
        auth.loginWithVerifiedEmail("anmol@example.com", "Ignored");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getName()).isEqualTo("Anmol");
        verify(ids, never()).next();
    }

    @Test
    void firstPhoneLoginCreatesAPasswordlessAccount() {
        when(users.findByPhone("+919876543210")).thenReturn(Optional.empty());
        when(ids.next()).thenReturn(2002L);

        UserAuthService.TokenPair pair = auth.loginWithVerifiedPhone("+919876543210", null);

        assertThat(pair.user().getId()).isEqualTo(2002L);
        assertThat(pair.user().getPhone()).isEqualTo("+919876543210");
        assertThat(pair.user().getEmail()).isNull();
        assertThat(pair.user().getPasswordHash()).isNull();
        assertThat(pair.user().getName()).isEqualTo("Member");
    }

    private static void assertStatus(Runnable call, HttpStatus status, String messagePart) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(UserServiceException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    assertThat(e.getMessage()).contains(messagePart);
                });
    }
}
