package com.buytogether.auth.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio account used when {@code buy-together.otp.sender=twilio}.
 * All three values come from the Twilio console.
 */
@ConfigurationProperties(prefix = "buy-together.otp.twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber
) {
}
