package com.example.userservice.account;

import com.example.userservice.common.SecureTokens;
import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.notify.AccountMailer;
import com.example.userservice.token.RefreshTokenService;
import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountLinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");
    private static final String BLOG = "https://chronicle09.vercel.app";
    private static final String COUPON = "https://coupon-voucher-service-frontend.vercel.app";

    private final FakeStore store = new FakeStore();
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private final AccountMailer mailer = mock(AccountMailer.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private AccountLinkService service;
    private User user;

    @BeforeEach
    void setUp() {
        UserServiceProperties.Links links = new UserServiceProperties.Links(List.of(BLOG, COUPON),
                Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofSeconds(60));
        service = new AccountLinkService(store, users, passwords, mailer, refreshTokens, links, Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User(5L, "Anmol", "anmol@example.com", passwords.encode("old-password"), NOW);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(users.findByEmail("anmol@example.com")).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void verificationLinkConfirmsTheEmailOnce() {
        service.sendVerification(user, COUPON + "/");
        String link = sentVerificationLink();

        assertThat(link).startsWith(COUPON + "/verify-email?token=");
        assertThat(service.confirmEmail(token(link)).isEmailVerified()).isTrue();
        assertThatThrownBy(() -> service.confirmEmail(token(link))).hasMessage(AccountLinkService.INVALID_LINK);
    }

    @Test
    void unknownAppAddressFallsBackToTheFirstAllowedOne() {
        service.sendVerification(user, "https://evil.example");

        assertThat(sentVerificationLink()).startsWith(BLOG + "/verify-email?token=");
    }

    @Test
    void confirmedAccountsGetNoEmailAndResendHasACooldown() {
        service.sendVerification(user, BLOG);
        assertThatThrownBy(() -> service.sendVerification(user, BLOG)).isInstanceOf(UserServiceException.class);

        user.setEmailVerified(true);
        User other = new User(6L, "Done", "done@example.com", "x", NOW);
        other.setEmailVerified(true);
        service.sendVerification(other, BLOG);
        verify(mailer, never()).sendVerificationLink(eq("done@example.com"), anyString(), anyString());
    }

    @Test
    void forgotPasswordSaysNothingAboutUnknownEmails() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("nobody@example.com", BLOG);

        verify(mailer, never()).sendPasswordResetLink(anyString(), anyString());
    }

    @Test
    void resetChangesThePasswordUnlocksAndLogsOutEverywhere() {
        user.setLockedUntil(NOW.plusSeconds(600));
        service.forgotPassword(" Anmol@Example.com ", BLOG);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendPasswordResetLink(eq("anmol@example.com"), link.capture());

        User changed = service.resetPassword(token(link.getValue()), "brand-new-password");

        assertThat(passwords.matches("brand-new-password", changed.getPasswordHash())).isTrue();
        assertThat(changed.getLockedUntil()).isNull();
        verify(refreshTokens).revokeAll(5L);
        assertThatThrownBy(() -> service.resetPassword(token(link.getValue()), "another-password")).hasMessage(AccountLinkService.INVALID_LINK);
    }

    @Test
    void verifyLinkCannotResetAPassword() {
        service.sendVerification(user, BLOG);

        assertThatThrownBy(() -> service.resetPassword(token(sentVerificationLink()), "brand-new-password"))
                .hasMessage(AccountLinkService.INVALID_LINK);
    }

    private String sentVerificationLink() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendVerificationLink(eq("anmol@example.com"), eq("Anmol"), link.capture());
        return link.getValue();
    }

    private static String token(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    static final class FakeStore implements AccountTokenStore {
        final List<AccountToken> tokens = new ArrayList<>();

        @Override
        public void save(AccountToken token) {
            tokens.add(token);
        }

        @Override
        public Optional<AccountToken> consume(String tokenHash, AccountToken.Purpose purpose, Instant now) {
            Optional<AccountToken> found = tokens.stream()
                    .filter(t -> t.getTokenHash().equals(tokenHash) && t.getPurpose() == purpose && t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                    .findFirst();
            found.ifPresent(t -> t.setUsedAt(now));
            return found;
        }

        @Override
        public void retireAll(Long userId, AccountToken.Purpose purpose, Instant now) {
            tokens.stream().filter(t -> t.getUserId().equals(userId) && t.getPurpose() == purpose && t.getUsedAt() == null)
                    .forEach(t -> t.setUsedAt(now));
        }
    }

    @Test
    void storesOnlyHashes() {
        service.sendVerification(user, BLOG);

        assertThat(store.tokens.get(0).getTokenHash()).isEqualTo(SecureTokens.sha256(token(sentVerificationLink())));
    }
}
