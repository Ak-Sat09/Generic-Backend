package com.example.userservice.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One account for every app. The id stays a number because blogs, likes and coupons already
 * store it that way. Fields added later are null on older documents, so readers treat
 * null as the default (for example a missing status means ACTIVE).
 */
@Document(collection = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    private Long id;
    private String name;
    /** Unique when present (see {@link AppUserIndexes}); null for phone-only accounts. */
    private String email;
    /** Null for accounts that only log in with OTP or Google. */
    private String passwordHash;
    /** +91XXXXXXXXXX. Unique when present. */
    private String phone;
    private Boolean emailVerified;
    private UserStatus status;
    private Integer failedLogins;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    public User(Long id, String name, String email, String passwordHash, Instant now) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
        this.status = UserStatus.ACTIVE;
        this.failedLogins = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** An account created by OTP or Google, without a password. */
    public static User passwordless(Long id, String name, String email, String phone, boolean emailVerified, Instant now) {
        User user = new User(id, name, email, null, now);
        user.setPhone(phone);
        user.setEmailVerified(emailVerified);
        return user;
    }

    public boolean isActive() {
        return status == null || status == UserStatus.ACTIVE;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public int failedLoginCount() {
        return failedLogins == null ? 0 : failedLogins;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }
}
