package com.example.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Settings under {@code user-service}. Keys and passwords only ever come from the environment.
 */
@ConfigurationProperties(prefix = "user-service")
public record UserServiceProperties(Jwt jwt, Login login, Links links, Mail mail, Otp otp, Sms sms, Google google) {

    /**
     * @param privateKey        RSA private key, PKCS#8, as base64 DER or PEM
     * @param generateIfMissing make a throwaway key when none is set; only for local runs and tests
     */
    public record Jwt(String issuer, Duration accessTokenTtl, Duration refreshTokenTtl,
                      String privateKey, boolean generateIfMissing) {
    }

    public record Login(int maxFailedAttempts, Duration lockDuration) {
    }

    /**
     * @param allowedOrigins frontends that email links may point to; anything else falls back to the first
     */
    public record Links(List<String> allowedOrigins, Duration verifyEmailTtl, Duration resetPasswordTtl,
                        Duration resendCooldown) {
    }

    /** @param sender "log" (development) or "smtp" */
    public record Mail(String sender, String from) {
    }

    public record Otp(int length, Duration ttl, Duration resendCooldown, int maxAttempts) {
    }

    /**
     * @param sender     "log" (development) or "twilio"
     * @param dailyLimit hard cap on SMS per day, to protect the bill
     */
    public record Sms(String sender, String twilioAccountSid, String twilioAuthToken, String twilioFromNumber, int dailyLimit) {
    }

    /** @param clientId OAuth client id from Google Cloud Console; Google login is off while empty */
    public record Google(String clientId) {
    }
}
