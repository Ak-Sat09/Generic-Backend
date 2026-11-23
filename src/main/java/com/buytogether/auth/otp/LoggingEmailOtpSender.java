package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: writes the code to the log instead of sending an email.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "buy-together.otp", name = "email-sender", havingValue = "log", matchIfMissing = true)
class LoggingEmailOtpSender implements EmailOtpSender {

    @Override
    public void send(EmailAddress email, String code) {
        log.info("OTP for {} is {}", email.masked(), code);
    }
}
