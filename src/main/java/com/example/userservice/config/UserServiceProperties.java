package com.example.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings under {@code user-service}. The signing key only ever comes from the environment.
 */
@ConfigurationProperties(prefix = "user-service")
public record UserServiceProperties(Jwt jwt, Login login) {

    /**
     * @param privateKey        RSA private key, PKCS#8, as base64 DER or PEM
     * @param generateIfMissing make a throwaway key when none is set; only for local runs and tests
     */
    public record Jwt(String issuer, Duration accessTokenTtl, Duration refreshTokenTtl,
                      String privateKey, boolean generateIfMissing) {
    }

    public record Login(int maxFailedAttempts, Duration lockDuration) {
    }
}
