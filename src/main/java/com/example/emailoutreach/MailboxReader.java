package com.example.emailoutreach;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Read-only access to the mailbox. Opened once per run and closed afterwards.
 */
public interface MailboxReader {

    Session open();

    interface Session extends AutoCloseable {

        /** Mail received on or after {@code since} in All Mail and Bin, excluding mail we sent. */
        List<IncomingMail> receivedSince(Instant since);

        /** To and Cc addresses of the message with this Message-ID, or empty if it can't be found. */
        Set<String> recipientsOf(String messageId);

        @Override
        void close();
    }
}
