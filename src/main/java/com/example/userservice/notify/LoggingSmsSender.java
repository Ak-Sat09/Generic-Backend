package com.example.userservice.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "user-service.sms", name = "sender", havingValue = "log", matchIfMissing = true)
class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String e164Phone, String text) {
        log.info("SMS to {}: {}", e164Phone, text);
    }
}
