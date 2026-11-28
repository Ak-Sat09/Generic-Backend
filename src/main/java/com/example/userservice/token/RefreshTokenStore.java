package com.example.userservice.token;

import java.time.Instant;
import java.util.Optional;

interface RefreshTokenStore {

    void save(RefreshToken token);

    Optional<RefreshToken> findByHash(String tokenHash);

    /** Marks the token used only if it is still fresh. False means someone else used it first. */
    boolean markUsed(String id, Instant at);

    void revokeFamily(String familyId, Instant at);

    void revokeAllForUser(Long userId, Instant at);
}
