package com.example.emailoutreach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly trigger for {@link ReplySyncService}. Off unless email-outreach.reply-sync.enabled=true.
 */
@Component
@ConditionalOnProperty(prefix = "email-outreach.reply-sync", name = "enabled", havingValue = "true")
class ReplySyncJob {

    private static final Logger log = LoggerFactory.getLogger(ReplySyncJob.class);

    private final ReplySyncService service;
    private final EmailOutreachProperties properties;

    ReplySyncJob(ReplySyncService service, EmailOutreachProperties properties) {
        this.service = service;
        this.properties = properties;
        log.info("Reply sync scheduled with cron '{}' ({})", properties.replySync().cron(), properties.replySync().zone());
    }

    @Scheduled(cron = "${email-outreach.reply-sync.cron}", zone = "${email-outreach.reply-sync.zone}")
    void runNightly() {
        if (!properties.gmail().hasCredentials()) {
            log.error("Reply sync not run: set EMAIL_OUTREACH_GMAIL_USERNAME and EMAIL_OUTREACH_GMAIL_APP_PASSWORD");
            return;
        }
        try {
            service.run();
        } catch (RuntimeException e) {
            // already logged and saved in sync_state; the next run starts from the last success
        }
    }
}
