package com.buytogether.auth.otp;

import com.buytogether.common.email.EmailAddress;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * Sends the code through any SMTP server configured under {@code spring.mail} (Gmail, Brevo, SES...).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "buy-together.otp", name = "email-sender", havingValue = "smtp")
class SmtpEmailOtpSender implements EmailOtpSender {

    private static final String SENDER_NAME = "Buy Together";

    private final JavaMailSender mailSender;
    private final String from;
    private final long ttlMinutes;

    SmtpEmailOtpSender(JavaMailSender mailSender,
                       @Value("${buy-together.mail.from:${spring.mail.username:}}") String from,
                       OtpProperties otp) {
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("spring.mail.username (or buy-together.mail.from) must be set when the email sender is smtp");
        }
        this.mailSender = mailSender;
        this.from = from;
        this.ttlMinutes = Math.max(1, otp.ttl().toMinutes());
    }

    @Override
    public void send(EmailAddress email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, SENDER_NAME);
            helper.setTo(email.value());
            helper.setSubject(code + " is your Buy Together login code");
            helper.setText(plainText(code), html(code));
            mailSender.send(message);
            log.info("OTP email sent to {}", email.masked());
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Could not send OTP email to {}: {}", email.masked(), e.getMessage());
            throw new OtpDeliveryException(e);
        }
    }

    private String plainText(String code) {
        return "Your Buy Together login code is " + code + ".\n\n"
                + "It expires in " + ttlMinutes + " minutes. If you did not ask for it, you can ignore this email.";
    }

    private String html(String code) {
        return """
                <div style="font-family:Arial,sans-serif;color:#1c232b;max-width:420px">
                  <p>Your Buy Together login code is</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px;margin:12px 0">%s</p>
                  <p style="color:#5b6670;font-size:13px">It expires in %d minutes. If you did not ask for it, you can ignore this email.</p>
                </div>
                """.formatted(code, ttlMinutes);
    }
}
