package com.buytogether.auth.otp;

/**
 * The SMS gateway refused or could not be reached.
 */
public class OtpDeliveryException extends RuntimeException {

    public OtpDeliveryException(Throwable cause) {
        super("OTP could not be delivered", cause);
    }
}
