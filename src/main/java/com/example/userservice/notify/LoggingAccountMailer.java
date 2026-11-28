package com.example.userservice.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "user-service.mail", name = "sender", havingValue = "log", matchIfMissing = true)
class LoggingAccountMailer implements AccountMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingAccountMailer.class);

    @Override
    public void sendVerificationLink(String to, String name, String link) {
        log.info("Verification link for {}: {}", to, link);
    }

    @Override
    public void sendPasswordResetLink(String to, String link) {
        log.info("Password reset link for {}: {}", to, link);
    }

    @Override
    public void sendLoginCode(String to, String code, long validMinutes) {
        log.info("Login code for {} is {}", to, code);
    }
}
