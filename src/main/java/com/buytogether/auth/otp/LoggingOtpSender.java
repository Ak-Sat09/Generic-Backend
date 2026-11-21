package com.buytogether.auth.otp;

import com.buytogether.common.phone.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: writes the code to the log instead of sending an SMS.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "buy-together.otp", name = "sender", havingValue = "log", matchIfMissing = true)
class LoggingOtpSender implements OtpSender {

    @Override
    public void send(PhoneNumber phone, String code) {
        log.info("OTP for {} is {}", phone.masked(), code);
    }
}
