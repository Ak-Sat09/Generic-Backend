package com.example.emailoutreach;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the email outreach module. Credentials come from environment variables only.
 */
@ConfigurationProperties(prefix = "email-outreach")
public record EmailOutreachProperties(String database, Gmail gmail, ReplySync replySync) {

    public record Gmail(String host, int port, String username, String appPassword) {

        boolean hasCredentials() {
            return username != null && !username.isBlank() && appPassword != null && !appPassword.isBlank();
        }
    }

    /**
     * @param enabled whether the nightly job is scheduled at all
     * @param cron    when it runs, in {@code zone}
     */
    public record ReplySync(boolean enabled, String cron, String zone) {
    }
}
