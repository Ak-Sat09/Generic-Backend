package com.buytogether.auth.otp;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.phone.PhoneNumber;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Issues and checks one time passwords.
 * Codes live only in memory (Guava cache with expire-after-write), stored as hashes,
 * so a restart simply means the user asks for a new code.
 */
@Service
public class OtpService {

    private static final long MAX_PENDING_CODES = 100_000;

    private final OtpProperties properties;
    private final OtpSender sender;
    private final OtpCodeGenerator generator;
    private final Clock clock;
    private final Cache<String, PendingOtp> pending;

    public OtpService(OtpProperties properties, OtpSender sender, OtpCodeGenerator generator, Clock clock) {
        this.properties = properties;
        this.sender = sender;
        this.generator = generator;
        this.clock = clock;
        this.pending = CacheBuilder.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(MAX_PENDING_CODES)
                .build();
    }

    public OtpChallenge send(PhoneNumber phone) {
        Instant now = clock.instant();
        PendingOtp existing = pending.getIfPresent(phone.value());
        if (existing != null) {
            Duration wait = Duration.between(now, existing.sentAt().plus(properties.resendCooldown()));
            if (!wait.isNegative() && !wait.isZero()) {
                throw new BusinessException(ErrorCode.OTP_RESEND_TOO_SOON,
                        "Please wait " + Math.max(1, wait.toSeconds()) + " seconds before asking for a new OTP");
            }
        }

        String code = generator.generate(properties.length());
        pending.put(phone.value(), new PendingOtp(hash(phone, code), now, new AtomicInteger()));
        try {
            sender.send(phone, code);
        } catch (OtpDeliveryException e) {
            // let the user try again straight away instead of waiting out the cooldown
            pending.invalidate(phone.value());
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED);
        }
        return new OtpChallenge(properties.ttl().toSeconds(), properties.resendCooldown().toSeconds());
    }

    public void verify(PhoneNumber phone, String code) {
        PendingOtp otp = pending.getIfPresent(phone.value());
        if (otp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (otp.attempts().get() >= properties.maxAttempts()) {
            pending.invalidate(phone.value());
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }
        if (!MessageDigest.isEqual(otp.hash(), hash(phone, code))) {
            int used = otp.attempts().incrementAndGet();
            if (used >= properties.maxAttempts()) {
                pending.invalidate(phone.value());
                throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
            }
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
        pending.invalidate(phone.value());
    }

    private static byte[] hash(PhoneNumber phone, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((phone.value() + ':' + code).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record PendingOtp(byte[] hash, Instant sentAt, AtomicInteger attempts) {
    }

    public record OtpChallenge(long expiresInSeconds, long resendAfterSeconds) {
    }
}
