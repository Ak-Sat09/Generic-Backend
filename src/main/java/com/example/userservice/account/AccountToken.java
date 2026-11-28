package com.example.userservice.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A one-time token behind an email link (verify email, reset password). Only its SHA-256 hash is kept,
 * and MongoDB deletes it once it expires.
 */
@Document(collection = "account_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AccountToken {

    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

    @Id
    private String id;
    @Indexed(unique = true)
    private String tokenHash;
    @Indexed
    private Long userId;
    private Purpose purpose;
    private Instant createdAt;
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
    private Instant usedAt;

    AccountToken(String tokenHash, Long userId, Purpose purpose, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.purpose = purpose;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
