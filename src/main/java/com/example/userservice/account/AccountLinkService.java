package com.example.userservice.account;

import com.example.userservice.account.AccountToken.Purpose;
import com.example.userservice.common.SecureTokens;
import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.notify.AccountMailer;
import com.example.userservice.token.RefreshTokenService;
import com.example.userservice.user.User;
import com.example.userservice.user.UserRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Email links: confirm an email address, and forgot / reset password. Links are one-time, expire,
 * and only point to frontends we know, so a link can't be redirected to someone else's site.
 */
@Service
public class AccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(AccountLinkService.class);
    static final String INVALID_LINK = "This link is invalid or has expired. Ask for a new one.";

    private final AccountTokenStore tokens;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccountMailer mailer;
    private final RefreshTokenService refreshTokens;
    private final UserServiceProperties.Links links;
    private final Clock clock;
    /** One email of each kind per account per cooldown, so the endpoints can't be used to flood an inbox. */
    private final Cache<String, Boolean> recentlySent;

    @Autowired
    AccountLinkService(AccountTokenStore tokens, UserRepository users, PasswordEncoder passwords, AccountMailer mailer,
                       RefreshTokenService refreshTokens, UserServiceProperties properties) {
        this(tokens, users, passwords, mailer, refreshTokens, properties.links(), Clock.systemUTC());
    }

    AccountLinkService(AccountTokenStore tokens, UserRepository users, PasswordEncoder passwords, AccountMailer mailer,
                       RefreshTokenService refreshTokens, UserServiceProperties.Links links, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.passwords = passwords;
        this.mailer = mailer;
        this.refreshTokens = refreshTokens;
        this.links = links;
        this.clock = clock;
        this.recentlySent = CacheBuilder.newBuilder().expireAfterWrite(links.resendCooldown()).maximumSize(50_000).build();
    }

    /** Sends the confirm-email link. Does nothing for accounts that are already confirmed or have no email. */
    public void sendVerification(User user, String appUrl) {
        if (user.getEmail() == null || user.isEmailVerified()) {
            return;
        }
        if (!firstInCooldown(Purpose.VERIFY_EMAIL, user.getId())) {
            throw new UserServiceException(HttpStatus.TOO_MANY_REQUESTS, "We just sent a link. Check your inbox, or try again in a minute.");
        }
        String raw = issue(user.getId(), Purpose.VERIFY_EMAIL);
        mailer.sendVerificationLink(user.getEmail(), user.getName(), origin(appUrl) + "/verify-email?token=" + raw);
    }

    public User confirmEmail(String rawToken) {
        AccountToken token = consume(rawToken, Purpose.VERIFY_EMAIL);
        User user = users.findById(token.getUserId()).orElseThrow(() -> new UserServiceException(HttpStatus.BAD_REQUEST, INVALID_LINK));
        user.setEmailVerified(true);
        user.setUpdatedAt(clock.instant());
        return users.save(user);
    }

    /**
     * Always answers the same way, whether or not the email belongs to an account, so nobody can use
     * it to find out who is registered.
     */
    public void forgotPassword(String rawEmail, String appUrl) {
        Optional<User> user = users.findByEmail(rawEmail.trim().toLowerCase(Locale.ROOT)).filter(User::isActive);
        if (user.isEmpty() || !firstInCooldown(Purpose.RESET_PASSWORD, user.get().getId())) {
            return;
        }
        String raw = issue(user.get().getId(), Purpose.RESET_PASSWORD);
        try {
            mailer.sendPasswordResetLink(user.get().getEmail(), origin(appUrl) + "/reset-password?token=" + raw);
        } catch (UserServiceException e) {
            log.warn("Password reset email for user {} not sent", user.get().getId());
        }
    }

    /** Sets the new password, unlocks the account and logs it out everywhere. */
    public User resetPassword(String rawToken, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "Password is too long (max 72 characters)");
        }
        AccountToken token = consume(rawToken, Purpose.RESET_PASSWORD);
        User user = users.findById(token.getUserId()).filter(User::isActive)
                .orElseThrow(() -> new UserServiceException(HttpStatus.BAD_REQUEST, INVALID_LINK));
        Instant now = clock.instant();
        user.setPasswordHash(passwords.encode(newPassword));
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        // the reset link reached this inbox, so the address is confirmed too
        user.setEmailVerified(true);
        user.setUpdatedAt(now);
        tokens.retireAll(user.getId(), Purpose.RESET_PASSWORD, now);
        refreshTokens.revokeAll(user.getId());
        return users.save(user);
    }

    /** The app's own address if we know it; otherwise the first allowed frontend. */
    String origin(String appUrl) {
        List<String> allowed = links.allowedOrigins();
        if (appUrl != null) {
            String trimmed = appUrl.trim().replaceAll("/+$", "");
            if (allowed.contains(trimmed)) {
                return trimmed;
            }
        }
        return allowed.get(0);
    }

    private String issue(Long userId, Purpose purpose) {
        Instant now = clock.instant();
        tokens.retireAll(userId, purpose, now);
        String raw = SecureTokens.random();
        tokens.save(new AccountToken(SecureTokens.sha256(raw), userId, purpose, now,
                now.plus(purpose == Purpose.VERIFY_EMAIL ? links.verifyEmailTtl() : links.resetPasswordTtl())));
        return raw;
    }

    private AccountToken consume(String rawToken, Purpose purpose) {
        return tokens.consume(SecureTokens.sha256(rawToken), purpose, clock.instant())
                .orElseThrow(() -> new UserServiceException(HttpStatus.BAD_REQUEST, INVALID_LINK));
    }

    private boolean firstInCooldown(Purpose purpose, Long userId) {
        return recentlySent.asMap().putIfAbsent(purpose + ":" + userId, Boolean.TRUE) == null;
    }
}
