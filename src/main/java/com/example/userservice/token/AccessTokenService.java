package com.example.userservice.token;

import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Short-lived access tokens signed with RS256. They keep the numeric {@code userId} claim that the
 * blog and coupon modules already read.
 */
@Component
public class AccessTokenService {

    static final String USER_ID_CLAIM = "userId";

    private final SigningKeys keys;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    AccessTokenService(SigningKeys keys, UserServiceProperties properties) {
        this(keys, properties.jwt().issuer(), properties.jwt().accessTokenTtl(), Clock.systemUTC());
    }

    AccessTokenService(SigningKeys keys, String issuer, Duration ttl, Clock clock) {
        this.keys = keys;
        this.issuer = issuer;
        this.ttl = ttl;
        this.clock = clock;
    }

    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        String token = Jwts.builder()
                .header().keyId(keys.keyId()).and()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim(USER_ID_CLAIM, user.getId())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(keys.privateKey(), Jwts.SIG.RS256)
                .compact();
        return new IssuedToken(token, expiresAt, ttl.toSeconds());
    }

    /** The user id in a valid, unexpired token from this service; empty for anything else. */
    public Optional<Long> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keys.publicKey())
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(userId(claims.get(USER_ID_CLAIM)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Long userId(Object claim) {
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public record IssuedToken(String token, Instant expiresAt, long expiresInSeconds) {
    }
}
