package com.example.userservice.notify;

import com.example.userservice.common.UserServiceException;
import com.example.userservice.config.UserServiceProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;

@Component
@ConditionalOnProperty(prefix = "user-service.mail", name = "sender", havingValue = "smtp")
class SmtpAccountMailer implements AccountMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpAccountMailer.class);
    private static final String SENDER_NAME = "Chronicle";

    private final JavaMailSender mailSender;
    private final String from;

    SmtpAccountMailer(JavaMailSender mailSender, UserServiceProperties properties) {
        if (!StringUtils.hasText(properties.mail().from())) {
            throw new IllegalStateException("user-service.mail.from (or spring.mail.username) must be set when the mail sender is smtp");
        }
        this.mailSender = mailSender;
        this.from = properties.mail().from();
    }

    @Override
    public void sendVerificationLink(String to, String name, String link) {
        String greeting = StringUtils.hasText(name) ? "Hi " + name + "," : "Hi,";
        send(to, "Confirm your email",
                greeting + "\n\nConfirm your email by opening this link:\n" + link + "\n\nThe link works for 24 hours.",
                paragraph(HtmlUtils.htmlEscape(greeting)) + paragraph("Confirm your email to finish setting up your account.")
                        + button(link, "Confirm email") + note("The link works for 24 hours. If you didn't create an account, ignore this email."));
    }

    @Override
    public void sendPasswordResetLink(String to, String link) {
        send(to, "Reset your password",
                "Someone asked to reset your password. Open this link to choose a new one:\n" + link
                        + "\n\nThe link works for 30 minutes. If it wasn't you, ignore this email.",
                paragraph("Someone asked to reset the password for this account.") + button(link, "Choose a new password")
                        + note("The link works for 30 minutes and only once. If it wasn't you, ignore this email; your password stays the same."));
    }

    @Override
    public void sendLoginCode(String to, String code, long validMinutes) {
        send(to, code + " is your login code",
                "Your login code is " + code + ". It expires in " + validMinutes + " minutes.",
                paragraph("Your login code is")
                        + "<p style=\"font-size:28px;font-weight:bold;letter-spacing:6px;margin:12px 0\">" + code + "</p>"
                        + note("It expires in " + validMinutes + " minutes. If you didn't ask for it, ignore this email."));
    }

    private void send(String to, String subject, String text, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, SENDER_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, "<div style=\"font-family:Arial,sans-serif;color:#1c232b;max-width:460px\">" + htmlBody + "</div>");
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Could not send '{}' email: {}", subject, e.getMessage());
            throw new UserServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Could not send the email right now, try again");
        }
    }

    private static String paragraph(String text) {
        return "<p>" + text + "</p>";
    }

    private static String note(String text) {
        return "<p style=\"color:#5b6670;font-size:13px\">" + text + "</p>";
    }

    private static String button(String link, String label) {
        String href = HtmlUtils.htmlEscape(link);
        return "<p><a href=\"" + href + "\" style=\"display:inline-block;background:#1c232b;color:#fff;padding:10px 18px;"
                + "border-radius:6px;text-decoration:none\">" + label + "</a></p>"
                + "<p style=\"color:#5b6670;font-size:12px;word-break:break-all\">" + href + "</p>";
    }
}
