package com.example.userservice.token;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One refresh token. Only a SHA-256 hash is stored, never the token itself. All tokens from one login
 * share a family, so a reused (stolen) token can end that whole login. MongoDB deletes documents
 * once {@code expiresAt} passes.
 */
@Document(collection = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    private String id;
    @Indexed(unique = true)
    private String tokenHash;
    @Indexed
    private String familyId;
    @Indexed
    private Long userId;
    private Instant createdAt;
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
    /** Set when this token was exchanged for a new one. */
    private Instant usedAt;
    /** Set on logout or when reuse was detected. */
    private Instant revokedAt;

    RefreshToken(String tokenHash, String familyId, Long userId, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
