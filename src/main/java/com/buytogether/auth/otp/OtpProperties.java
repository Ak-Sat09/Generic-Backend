package com.buytogether.auth.otp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "buy-together.otp")
public record OtpProperties(
        @Min(4) @Max(8) int length,
        @NotNull Duration ttl,
        @NotNull Duration resendCooldown,
        @Min(1) int maxAttempts,
        String sender
) {
}
