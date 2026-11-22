package com.buytogether.common.phone;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;

import java.util.regex.Pattern;

/**
 * An Indian mobile number in E.164 form (+91XXXXXXXXXX).
 * Accepts the usual ways people type it: "98765 43210", "+91-9876543210", "09876543210".
 */
public record PhoneNumber(String value) {

    private static final Pattern MOBILE = Pattern.compile("[6-9]\\d{9}");
    private static final String COUNTRY_CODE = "91";

    public static PhoneNumber parse(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.INVALID_PHONE);
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith(COUNTRY_CODE)) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!MOBILE.matcher(digits).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE);
        }
        return new PhoneNumber("+" + COUNTRY_CODE + digits);
    }

    /** Safe to print in logs: +91******3210 */
    public String masked() {
        return value.substring(0, 3) + "******" + value.substring(value.length() - 4);
    }

    @Override
    public String toString() {
        return masked();
    }
}
