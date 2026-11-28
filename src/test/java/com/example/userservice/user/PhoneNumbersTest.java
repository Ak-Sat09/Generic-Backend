package com.example.userservice.user;

import com.example.userservice.common.UserServiceException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "98765 43210", "+91-9876543210", "+91 98765 43210", "09876543210", "919876543210"})
    void acceptsCommonFormats(String raw) {
        assertThat(PhoneNumbers.normalize(raw)).isEqualTo("+919876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "5876543210", "+1 4155552671", "98765432101"})
    void rejectsOthers(String raw) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(raw)).isInstanceOf(UserServiceException.class);
    }

    @Test
    void masksForLogs() {
        assertThat(PhoneNumbers.masked("+919876543210")).isEqualTo("+91******3210");
    }
}
