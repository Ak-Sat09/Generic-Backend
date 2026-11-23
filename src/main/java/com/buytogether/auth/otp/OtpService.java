package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;
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
import java.util.function.Consumer;

/**
 * Issues and checks one time passwords.
 * Codes live only in memory (Guava cache with expire-after-write), stored as hashes,
 * so a restart simply means the user asks for a new code.
 */
@Service
public class OtpService {

    private static final long MAX_PENDING_CODES = 100_000;

    private final OtpProperties properties;
    private final OtpSender smsSender;
    private final EmailOtpSender emailSender;
    private final OtpCodeGenerator generator;
    private final Clock clock;
    private final Cache<String, PendingOtp> pending;

    public OtpService(OtpProperties properties, OtpSender smsSender, EmailOtpSender emailSender,
                      OtpCodeGenerator generator, Clock clock) {
        this.properties = properties;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.generator = generator;
        this.clock = clock;
        this.pending = CacheBuilder.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(MAX_PENDING_CODES)
                .build();
    }

    public OtpChallenge send(PhoneNumber phone) {
        return issue(phoneKey(phone), code -> smsSender.send(phone, code));
    }

    public OtpChallenge send(EmailAddress email) {
        return issue(emailKey(email), code -> emailSender.send(email, code));
    }

    public void verify(PhoneNumber phone, String code) {
        check(phoneKey(phone), code);
    }

    public void verify(EmailAddress email, String code) {
        check(emailKey(email), code);
    }

    private OtpChallenge issue(String key, Consumer<String> delivery) {
        Instant now = clock.instant();
        PendingOtp existing = pending.getIfPresent(key);
        if (existing != null) {
            Duration wait = Duration.between(now, existing.sentAt().plus(properties.resendCooldown()));
            if (!wait.isNegative() && !wait.isZero()) {
                throw new BusinessException(ErrorCode.OTP_RESEND_TOO_SOON,
                        "Please wait " + Math.max(1, wait.toSeconds()) + " seconds before asking for a new OTP");
            }
        }

        String code = generator.generate(properties.length());
        pending.put(key, new PendingOtp(hash(key, code), now, new AtomicInteger()));
        try {
            delivery.accept(code);
        } catch (OtpDeliveryException e) {
            // let the user try again straight away instead of waiting out the cooldown
            pending.invalidate(key);
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED);
        }
        return new OtpChallenge(properties.ttl().toSeconds(), properties.resendCooldown().toSeconds());
    }

    private void check(String key, String code) {
        PendingOtp otp = pending.getIfPresent(key);
        if (otp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (otp.attempts().get() >= properties.maxAttempts()) {
            pending.invalidate(key);
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }
        if (!MessageDigest.isEqual(otp.hash(), hash(key, code))) {
            int used = otp.attempts().incrementAndGet();
            if (used >= properties.maxAttempts()) {
                pending.invalidate(key);
                throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
            }
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
        pending.invalidate(key);
    }

    private static String phoneKey(PhoneNumber phone) {
        return "phone:" + phone.value();
    }

    private static String emailKey(EmailAddress email) {
        return "email:" + email.value();
    }

    private static byte[] hash(String key, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((key + ':' + code).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record PendingOtp(byte[] hash, Instant sentAt, AtomicInteger attempts) {
    }

    public record OtpChallenge(long expiresInSeconds, long resendAfterSeconds) {
    }
}
