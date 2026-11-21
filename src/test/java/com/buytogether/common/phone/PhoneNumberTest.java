package com.buytogether.common.phone;

import com.buytogether.common.error.BusinessException;
import com.buytogether.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+91 98765 43210", "+91-9876543210", "919876543210", "09876543210"})
    void acceptsCommonFormats(String input) {
        assertThat(PhoneNumber.parse(input).value()).isEqualTo("+919876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "5876543210", "98765432101", "abcdefghij"})
    void rejectsInvalidNumbers(String input) {
        assertThatThrownBy(() -> PhoneNumber.parse(input)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PHONE));
    }

    @Test
    void masksMiddleDigits() {
        assertThat(PhoneNumber.parse("9876543210").masked()).isEqualTo("+91******3210");
    }
}
