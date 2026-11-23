package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;
import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import com.buytogether.common.phone.PhoneNumber;
import com.buytogether.support.MutableClock;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private static final PhoneNumber PHONE = PhoneNumber.parse("9876543210");

    private static final EmailAddress EMAIL = EmailAddress.parse("Anmol@Example.com");

    private final OtpSender sender = mock(OtpSender.class);
    private final EmailOtpSender emailSender = mock(EmailOtpSender.class);
    private final OtpCodeGenerator generator = mock(OtpCodeGenerator.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        OtpProperties properties = new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 3, "log");
        otpService = new OtpService(properties, sender, emailSender, generator, clock);
        when(generator.generate(anyInt())).thenReturn("123456", "654321");
    }

    @Test
    void sendsCodeAndAcceptsIt() {
        otpService.send(PHONE);

        verify(sender).send(PHONE, "123456");
        assertThatCode(() -> otpService.verify(PHONE, "123456")).doesNotThrowAnyException();
    }

    @Test
    void codeCannotBeUsedTwice() {
        otpService.send(PHONE);
        otpService.verify(PHONE, "123456");

        assertError(() -> otpService.verify(PHONE, "123456"), ErrorCode.OTP_EXPIRED);
    }

    @Test
    void blocksResendDuringCooldown() {
        otpService.send(PHONE);
        clock.advance(Duration.ofSeconds(30));

        assertError(() -> otpService.send(PHONE), ErrorCode.OTP_RESEND_TOO_SOON);
    }

    @Test
    void allowsResendAfterCooldownAndOnlyNewCodeWorks() {
        otpService.send(PHONE);
        clock.advance(Duration.ofSeconds(61));
        otpService.send(PHONE);

        verify(sender, times(2)).send(eq(PHONE), anyString());
        assertError(() -> otpService.verify(PHONE, "123456"), ErrorCode.OTP_INVALID);
        assertThatCode(() -> otpService.verify(PHONE, "654321")).doesNotThrowAnyException();
    }

    @Test
    void locksAfterTooManyWrongAttempts() {
        otpService.send(PHONE);

        assertError(() -> otpService.verify(PHONE, "000000"), ErrorCode.OTP_INVALID);
        assertError(() -> otpService.verify(PHONE, "000001"), ErrorCode.OTP_INVALID);
        assertError(() -> otpService.verify(PHONE, "000002"), ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        // the right code no longer helps, a new one must be requested
        assertError(() -> otpService.verify(PHONE, "123456"), ErrorCode.OTP_EXPIRED);
    }

    @Test
    void reportsChallengeTimings() {
        OtpService.OtpChallenge challenge = otpService.send(PHONE);

        assertThat(challenge.expiresInSeconds()).isEqualTo(300);
        assertThat(challenge.resendAfterSeconds()).isEqualTo(60);
    }

    @Test
    void failedDeliveryAllowsImmediateRetry() {
        doThrow(new OtpDeliveryException(new RuntimeException("gateway down")))
                .doNothing()
                .when(sender).send(eq(PHONE), anyString());

        assertError(() -> otpService.send(PHONE), ErrorCode.OTP_DELIVERY_FAILED);
        assertError(() -> otpService.verify(PHONE, "123456"), ErrorCode.OTP_EXPIRED);

        assertThatCode(() -> otpService.send(PHONE)).doesNotThrowAnyException();
        assertThatCode(() -> otpService.verify(PHONE, "654321")).doesNotThrowAnyException();
    }

    @Test
    void sendsEmailCodeAndAcceptsIt() {
        otpService.send(EMAIL);

        verify(emailSender).send(EMAIL, "123456");
        assertThatCode(() -> otpService.verify(EMAIL, "123456")).doesNotThrowAnyException();
    }

    @Test
    void phoneAndEmailCodesAreKeptApart() {
        otpService.send(PHONE);
        otpService.send(EMAIL);

        assertError(() -> otpService.verify(EMAIL, "123456"), ErrorCode.OTP_INVALID);
        assertThatCode(() -> otpService.verify(PHONE, "123456")).doesNotThrowAnyException();
        assertThatCode(() -> otpService.verify(EMAIL, "654321")).doesNotThrowAnyException();
    }

    private static void assertError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
