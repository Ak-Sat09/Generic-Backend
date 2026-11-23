package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;

/**
 * Delivers a one time password by email. Chosen with {@code buy-together.otp.email-sender}.
 */
public interface EmailOtpSender {

    void send(EmailAddress email, String code);
}
