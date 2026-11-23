package com.buytogether.common.email;

import com.buytogether.common.error.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void normalisesCaseAndSpaces() {
        assertThat(EmailAddress.parse("  Anmol.Mehla@Gmail.COM ").value()).isEqualTo("anmol.mehla@gmail.com");
    }

    @Test
    void masksForLogs() {
        assertThat(EmailAddress.parse("anmol@gmail.com").masked()).isEqualTo("an***@gmail.com");
        assertThat(EmailAddress.parse("a@gmail.com").masked()).isEqualTo("a***@gmail.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "anmol", "anmol@", "@gmail.com", "anmol@gmail", "an mol@gmail.com", "a@b@gmail.com"})
    void rejectsInvalidAddresses(String raw) {
        assertThatThrownBy(() -> EmailAddress.parse(raw)).isInstanceOf(BusinessException.class);
    }
}
