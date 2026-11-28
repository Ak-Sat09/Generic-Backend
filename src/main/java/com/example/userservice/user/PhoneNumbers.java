package com.example.userservice.user;

import com.example.userservice.common.UserServiceException;
import org.springframework.http.HttpStatus;

/**
 * Indian mobile numbers in one shape: +91 followed by 10 digits starting 6 to 9.
 * Accepts "98765 43210", "+91-9876543210", "09876543210" and similar.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[\\s()\\-.]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!digits.matches("[6-9]\\d{9}")) {
            throw new UserServiceException(HttpStatus.BAD_REQUEST, "Enter a valid 10 digit Indian mobile number");
        }
        return "+91" + digits;
    }

    /** +91******3210, safe for logs. */
    public static String masked(String e164) {
        return e164.substring(0, 3) + "******" + e164.substring(e164.length() - 4);
    }
}
