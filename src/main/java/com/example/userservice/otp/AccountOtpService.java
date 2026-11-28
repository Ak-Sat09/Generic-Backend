package com.example.userservice.otp;

import com.example.userservice.common.SecureTokens;
import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.notify.AccountMailer;
import com.example.userservice.notify.SmsSender;
import com.example.userservice.user.PhoneNumbers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-time login codes by SMS or email. Codes live only in memory, stored as hashes, with a resend
 * cooldown, a limit on wrong tries and a daily SMS cap that protects the Twilio bill.
 */
@Service
public class AccountOtpService {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final UserServiceProperties.Otp settings;
    private final int dailySmsLimit;
    private final SmsSender sms;
    private final AccountMailer mailer;
    private final Clock clock;
    private final Cache<String, Pending> pending;
    private final Object smsCounterLock = new Object();
    private LocalDate smsDay;
    private int smsSentToday;

    @Autowired
    AccountOtpService(UserServiceProperties properties, SmsSender sms, AccountMailer mailer) {
        this(properties.otp(), properties.sms().dailyLimit(), sms, mailer, Clock.systemUTC());
    }

    AccountOtpService(UserServiceProperties.Otp settings, int dailySmsLimit, SmsSender sms, AccountMailer mailer, Clock clock) {
        this.settings = settings;
        this.dailySmsLimit = dailySmsLimit;
        this.sms = sms;
        this.mailer = mailer;
        this.clock = clock;
        this.pending = CacheBuilder.newBuilder().expireAfterWrite(settings.ttl()).maximumSize(100_000).build();
    }

    public Challenge send(Destination to) {
        Instant now = clock.instant();
        Pending existing = pending.getIfPresent(to.key());
        if (existing != null) {
            Duration wait = Duration.between(now, existing.sentAt().plus(settings.resendCooldown()));
            if (wait.isPositive()) {
                throw new UserServiceException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + Math.max(1, wait.toSeconds()) + " seconds before asking for a new code");
            }
        }
        if (to.channel() == Channel.PHONE) {
            reserveSms();
        }
        String code = SecureTokens.digits(settings.length());
        pending.put(to.key(), new Pending(SecureTokens.sha256(to.key() + ":" + code), now, new AtomicInteger()));
        long minutes = Math.max(1, settings.ttl().toMinutes());
        try {
            if (to.channel() == Channel.PHONE) {
                sms.send(to.value(), code + " is your Chronicle login code. It expires in " + minutes + " minutes.");
            } else {
                mailer.sendLoginCode(to.value(), code, minutes);
            }
        } catch (UserServiceException e) {
            pending.invalidate(to.key()); // let them retry straight away
            throw e;
        }
        return new Challenge(settings.ttl().toSeconds(), settings.resendCooldown().toSeconds());
    }

    public void verify(Destination to, String code) {
        Pending otp = pending.getIfPresent(to.key());
        if (otp == null) {
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "The code has expired. Ask for a new one.");
        }
        if (otp.attempts().incrementAndGet() > settings.maxAttempts()) {
            pending.invalidate(to.key());
            throw new UserServiceException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong tries. Ask for a new code.");
        }
        byte[] expected = otp.hash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = SecureTokens.sha256(to.key() + ":" + (code == null ? "" : code.trim())).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "That code is not right");
        }
        pending.invalidate(to.key()); // one use only
    }

    private void reserveSms() {
        synchronized (smsCounterLock) {
            LocalDate today = LocalDate.now(clock.withZone(INDIA));
            if (!today.equals(smsDay)) {
                smsDay = today;
                smsSentToday = 0;
            }
            if (smsSentToday >= dailySmsLimit) {
                throw new UserServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                        "SMS codes are not available right now. Log in with email or Google instead.");
            }
            smsSentToday++;
        }
    }

    public enum Channel { PHONE, EMAIL }

    /** Where a code goes, normalised: +91XXXXXXXXXX or a lower-case email. */
    public record Destination(Channel channel, String value) {

        public static Destination of(String channel, String raw) {
            if ("phone".equalsIgnoreCase(channel)) {
                return new Destination(Channel.PHONE, PhoneNumbers.normalize(raw));
            }
            if ("email".equalsIgnoreCase(channel)) {
                String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
                if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}") || email.length() > 254) {
                    throw new UserServiceException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
                }
                return new Destination(Channel.EMAIL, email);
            }
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "Channel must be phone or email");
        }

        String key() {
            return channel + ":" + value;
        }
    }

    public record Challenge(long expiresInSeconds, long resendAfterSeconds) {
    }

    private record Pending(String hash, Instant sentAt, AtomicInteger attempts) {
    }
}
