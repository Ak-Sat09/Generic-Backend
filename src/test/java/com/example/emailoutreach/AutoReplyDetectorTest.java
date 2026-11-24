package com.example.emailoutreach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AutoReplyDetectorTest {

    @Test
    void plainReplyIsReal() {
        assertThat(AutoReplyDetector.isAutomatic(null, false, null, "Re: Application for Java Developer")).isFalse();
        assertThat(AutoReplyDetector.isAutomatic("no", false, null, "Re: your resume")).isFalse();
    }

    @Test
    void standardHeadersMarkAutomaticMail() {
        assertThat(AutoReplyDetector.isAutomatic("auto-replied", false, null, "Re: hello")).isTrue();
        assertThat(AutoReplyDetector.isAutomatic(null, true, null, "Re: hello")).isTrue();
        assertThat(AutoReplyDetector.isAutomatic(null, false, "auto_reply", "Re: hello")).isTrue();
        assertThat(AutoReplyDetector.isAutomatic(null, false, "bulk", "Newsletter")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Automatic reply: Application", "Out of Office: back Monday", "Auto-Reply: thanks", "OOO: travelling"})
    void outOfOfficeSubjectsAreAutomatic(String subject) {
        assertThat(AutoReplyDetector.isAutomatic(null, false, null, subject)).isTrue();
    }

    @Test
    void bounceAndNoReplySendersAreSystem() {
        assertThat(AutoReplyDetector.isSystemSender("mailer-daemon@googlemail.com")).isTrue();
        assertThat(AutoReplyDetector.isSystemSender("no-reply@naukri.com")).isTrue();
        assertThat(AutoReplyDetector.isSystemSender("hr@tcs.com")).isFalse();
    }

    @Test
    void takesTheFirstMessageIdFromInReplyTo() {
        assertThat(GmailMailboxReader.firstMessageId("<abc@mail.gmail.com> <def@x>")).isEqualTo("<abc@mail.gmail.com>");
        assertThat(GmailMailboxReader.firstMessageId(null)).isNull();
        assertThat(GmailMailboxReader.firstMessageId("garbage")).isNull();
    }
}
