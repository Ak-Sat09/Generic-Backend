package com.buytogether.common.email;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A normalised email address (trimmed, lower case) so the same inbox always maps to one account.
 */
public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern SHAPE = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}");

    public static EmailAddress parse(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL);
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > MAX_LENGTH || !SHAPE.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL);
        }
        return new EmailAddress(email);
    }

    /** Safe to print in logs: an***@gmail.com */
    public String masked() {
        int at = value.indexOf('@');
        return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
    }

    @Override
    public String toString() {
        return masked();
    }
}
