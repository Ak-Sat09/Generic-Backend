package com.buytogether.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "buy-together.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET environment variable is required") String secret,
        @NotNull Duration ttl,
        @NotBlank String issuer
) {
}
