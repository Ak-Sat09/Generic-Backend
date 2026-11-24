package com.example.emailoutreach;

import java.util.List;
import java.util.Locale;

/**
 * Tells real replies apart from out-of-office and other automatic mail, using standard headers
 * (RFC 3834 Auto-Submitted and the common X- headers) and, as a fallback, well-known subjects.
 */
final class AutoReplyDetector {

    private static final List<String> AUTO_SUBJECTS = List.of(
            "automatic reply", "auto reply", "auto-reply", "autoreply", "auto response", "auto-response",
            "out of office", "out of the office", "ooo:", "away from the office", "on leave");

    private AutoReplyDetector() {
    }

    static boolean isAutomatic(String autoSubmitted, boolean hasXAutoReplyHeader, String precedence, String subject) {
        if (autoSubmitted != null && !autoSubmitted.isBlank() && !"no".equalsIgnoreCase(autoSubmitted.trim())) {
            return true;
        }
        if (hasXAutoReplyHeader) {
            return true;
        }
        if (precedence != null) {
            String value = precedence.trim().toLowerCase(Locale.ROOT);
            if (value.equals("auto_reply") || value.equals("bulk") || value.equals("junk") || value.equals("list")) {
                return true;
            }
        }
        if (subject != null) {
            String value = subject.trim().toLowerCase(Locale.ROOT);
            return AUTO_SUBJECTS.stream().anyMatch(value::startsWith);
        }
        return false;
    }

    /** Bounce and system senders never count as a person replying. */
    static boolean isSystemSender(String email) {
        String local = email.substring(0, Math.max(0, email.indexOf('@'))).toLowerCase(Locale.ROOT);
        return local.equals("mailer-daemon") || local.equals("postmaster") || local.startsWith("no-reply")
                || local.startsWith("noreply") || local.startsWith("do-not-reply") || local.startsWith("donotreply");
    }
}
