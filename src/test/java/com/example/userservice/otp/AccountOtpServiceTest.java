package com.example.userservice.otp;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import com.example.userservice.notify.AccountMailer;
import com.example.userservice.notify.SmsSender;
import com.example.userservice.otp.AccountOtpService.Destination;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountOtpServiceTest {

    private final SmsSender sms = mock(SmsSender.class);
    private final AccountMailer mailer = mock(AccountMailer.class);
    private final MovableClock clock = new MovableClock(Instant.parse("2026-11-28T10:00:00Z"));
    private final AccountOtpService service = new AccountOtpService(
            new UserServiceProperties.Otp(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 3), 2, sms, mailer, clock);

    private final Destination phone = Destination.of("phone", "98765 43210");
    private final Destination email = Destination.of("email", " Anmol@Example.com ");

    @Test
    void normalisesDestinations() {
        assertThat(phone.value()).isEqualTo("+919876543210");
        assertThat(email.value()).isEqualTo("anmol@example.com");
        assertThatThrownBy(() -> Destination.of("phone", "12345")).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> Destination.of("fax", "x")).isInstanceOf(UserServiceException.class);
    }

    @Test
    void smsCodeWorksOnce() {
        service.send(phone);
        String code = smsCode();

        assertThatCode(() -> service.verify(phone, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verify(phone, code)).hasMessageContaining("expired");
    }

    @Test
    void emailCodeGoesByEmail() {
        service.send(email);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendLoginCode(eq("anmol@example.com"), code.capture(), eq(5L));

        assertThatCode(() -> service.verify(email, code.getValue())).doesNotThrowAnyException();
    }

    @Test
    void resendNeedsACooldown() {
        service.send(phone);
        assertThatThrownBy(() -> service.send(phone)).hasMessageContaining("wait");

        clock.now = clock.now.plusSeconds(61);
        assertThatCode(() -> service.send(phone)).doesNotThrowAnyException();
    }

    @Test
    void tooManyWrongTriesEndTheCode() {
        service.send(phone);
        String code = smsCode();
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verify(phone, "000000")).isInstanceOf(UserServiceException.class);
        }

        assertThatThrownBy(() -> service.verify(phone, code)).isInstanceOf(UserServiceException.class);
    }

    @Test
    void dailySmsCapProtectsTheBill() {
        service.send(phone);
        service.send(Destination.of("phone", "9876500001"));

        assertThatThrownBy(() -> service.send(Destination.of("phone", "9876500002")))
                .isInstanceOfSatisfying(UserServiceException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatCode(() -> service.send(email)).doesNotThrowAnyException();
    }

    @Test
    void failedDeliveryAllowsAnImmediateRetry() {
        doThrow(new UserServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down")).when(mailer).sendLoginCode(anyString(), anyString(), anyLong());

        assertThatThrownBy(() -> service.send(email)).isInstanceOf(UserServiceException.class);
        assertThatThrownBy(() -> service.send(email)).hasMessage("down"); // no cooldown error
    }

    private String smsCode() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(sms).send(eq("+919876543210"), text.capture());
        return text.getValue().substring(0, 6);
    }

    static final class MovableClock extends Clock {
        Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
