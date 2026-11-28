package com.example.userservice.token;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The one place every module asks "who is this token for?". Accepts the new RS256 access token and,
 * during the move, the old HS256 token.
 */
@Component
public class AccessTokenVerifier {

    private static final String BEARER = "Bearer ";

    private final AccessTokenService accessTokens;
    private final LegacyTokenService legacyTokens;

    public AccessTokenVerifier(AccessTokenService accessTokens, LegacyTokenService legacyTokens) {
        this.accessTokens = accessTokens;
        this.legacyTokens = legacyTokens;
    }

    /** Takes a raw token or a full "Bearer ..." header value. */
    public Optional<Long> userId(String tokenOrHeader) {
        if (tokenOrHeader == null || tokenOrHeader.isBlank()) {
            return Optional.empty();
        }
        String token = tokenOrHeader.startsWith(BEARER) ? tokenOrHeader.substring(BEARER.length()).trim() : tokenOrHeader.trim();
        Optional<Long> userId = accessTokens.verify(token);
        return userId.isPresent() ? userId : legacyTokens.verify(token);
    }
}
