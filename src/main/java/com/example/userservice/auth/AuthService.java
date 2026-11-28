package com.example.userservice.auth;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.token.AccessTokenService;
import com.example.userservice.token.AccessTokenService.IssuedToken;
import com.example.userservice.token.RefreshTokenService;
import com.example.userservice.token.RefreshTokenService.IssuedRefreshToken;
import com.example.userservice.token.RefreshTokenService.Rotation;
import com.example.userservice.user.User;
import com.example.userservice.user.UserIdSequence;
import com.example.userservice.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Registration, password login with temporary lockout, and the token pair every login ends with.
 */
@Service
public class AuthService {

    static final String INVALID_CREDENTIALS = "Invalid email or password";
    /** BCrypt only uses the first 72 bytes of a password, so longer ones are refused. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final UserIdSequence ids;
    private final PasswordEncoder passwords;
    private final AccessTokenService accessTokens;
    private final RefreshTokenService refreshTokens;
    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    /** Checked when the email is unknown, so both cases take the same time. */
    private final String dummyHash;

    @Autowired
    AuthService(UserRepository users, UserIdSequence ids, PasswordEncoder passwords, AccessTokenService accessTokens,
                RefreshTokenService refreshTokens, UserServiceProperties properties) {
        this(users, ids, passwords, accessTokens, refreshTokens,
                properties.login().maxFailedAttempts(), properties.login().lockDuration(), Clock.systemUTC());
    }

    AuthService(UserRepository users, UserIdSequence ids, PasswordEncoder passwords, AccessTokenService accessTokens,
                RefreshTokenService refreshTokens, int maxFailedAttempts, Duration lockDuration, Clock clock) {
        this.users = users;
        this.ids = ids;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
        this.dummyHash = passwords.encode("timing-equaliser-not-a-real-password");
    }

    public User register(String name, String rawEmail, String password) {
        String email = normalize(rawEmail);
        requireUsablePassword(password);
        if (users.existsByEmail(email)) {
            throw new UserServiceException(HttpStatus.CONFLICT, "Email is already registered");
        }
        try {
            return users.save(new User(ids.next(), name.trim(), email, passwords.encode(password), clock.instant()));
        } catch (DuplicateKeyException race) {
            throw new UserServiceException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    /**
     * Checks email and password. After too many wrong passwords the account is locked for a while,
     * even for the right password. Unknown emails and wrong passwords get the same answer.
     */
    public User authenticate(String rawEmail, String password) {
        Instant now = clock.instant();
        User user = users.findByEmail(normalize(rawEmail)).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            passwords.matches(password, dummyHash);
            throw UserServiceException.unauthorized(INVALID_CREDENTIALS);
        }
        if (user.isLockedAt(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new UserServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Try again in " + minutes + " minutes");
        }
        if (!passwords.matches(password, user.getPasswordHash())) {
            recordFailure(user, now);
            throw UserServiceException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new UserServiceException(HttpStatus.FORBIDDEN, "This account is disabled");
        }
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        return users.save(user);
    }

    public TokenPair login(String email, String password) {
        return tokensFor(authenticate(email, password));
    }

    public TokenPair refresh(String refreshToken) {
        Rotation rotation = refreshTokens.rotate(refreshToken);
        User user = users.findById(rotation.userId())
                .filter(User::isActive)
                .orElseThrow(() -> UserServiceException.unauthorized("Your session has ended, please log in again"));
        IssuedToken access = accessTokens.issue(user);
        return new TokenPair(access, rotation.refreshToken(), user);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    public void logoutEverywhere(Long userId) {
        refreshTokens.revokeAll(userId);
    }

    public User get(Long userId) {
        return users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> UserServiceException.unauthorized("Please log in again"));
    }

    private TokenPair tokensFor(User user) {
        return new TokenPair(accessTokens.issue(user), refreshTokens.issue(user.getId()), user);
    }

    private void recordFailure(User user, Instant now) {
        int failures = user.failedLoginCount() + 1;
        if (failures >= maxFailedAttempts) {
            user.setLockedUntil(now.plus(lockDuration));
            failures = 0;
        }
        user.setFailedLogins(failures);
        user.setUpdatedAt(now);
        users.save(user);
    }

    private static void requireUsablePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "Password is too long (max 72 characters)");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record TokenPair(IssuedToken accessToken, IssuedRefreshToken refreshToken, User user) {
    }
}
