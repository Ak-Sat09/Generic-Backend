package com.buytogether.auth.otp;

import com.buytogether.common.phone.PhoneNumber;

/**
 * Delivers a one time password to the user. Swap the implementation with the
 * {@code buy-together.otp.sender} property (for example a real SMS gateway in production).
 */
public interface OtpSender {

    void send(PhoneNumber phone, String code);
}
