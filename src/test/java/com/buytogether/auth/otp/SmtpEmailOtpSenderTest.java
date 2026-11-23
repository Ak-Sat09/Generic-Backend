package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailOtpSenderTest {

    private static final EmailAddress EMAIL = EmailAddress.parse("user@example.com");
    private final OtpProperties otp = new OtpProperties(6, Duration.ofMinutes(5), Duration.ofSeconds(60), 3, "log");
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsTheCodeFromTheConfiguredAddress() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        new SmtpEmailOtpSender(mailSender, "sender@example.com", otp).send(EMAIL, "482913");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getSubject()).contains("482913");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(message.getFrom()[0].toString()).contains("sender@example.com");
    }

    @Test
    void smtpFailureBecomesDeliveryFailure() {
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        doThrow(new MailSendException("auth failed")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> new SmtpEmailOtpSender(mailSender, "sender@example.com", otp).send(EMAIL, "482913"))
                .isInstanceOf(OtpDeliveryException.class);
    }

    @Test
    void refusesToStartWithoutSenderAddress() {
        assertThatThrownBy(() -> new SmtpEmailOtpSender(mailSender, "", otp))
                .isInstanceOf(IllegalStateException.class);
    }
}
