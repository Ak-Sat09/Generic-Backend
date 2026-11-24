package com.example.emailoutreach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Marks contacts RESPONDED when a real reply from them shows up in Gmail.
 * Starts from the last successful run, so a missed night is caught up on the next one.
 */
@Service
public class ReplySyncService {

    private static final Logger log = LoggerFactory.getLogger(ReplySyncService.class);
    private static final Duration FIRST_RUN_WINDOW = Duration.ofHours(24);
    // IMAP searches by whole day, so look one extra day back; marking RESPONDED twice changes nothing
    private static final Duration DAY_OVERLAP = Duration.ofDays(1);

    private final MailboxReader mailbox;
    private final OutreachStore store;
    private final Clock clock;

    @Autowired
    ReplySyncService(MailboxReader mailbox, OutreachStore store) {
        this(mailbox, store, Clock.systemUTC());
    }

    ReplySyncService(MailboxReader mailbox, OutreachStore store, Clock clock) {
        this.mailbox = mailbox;
        this.store = store;
        this.clock = clock;
    }

    public Result run() {
        if (store.backfillRunning()) {
            log.info("Reply sync skipped: backfill is still running");
            return Result.SKIPPED;
        }
        Instant now = clock.instant();
        Instant since = store.lastSuccessAt().orElse(now.minus(FIRST_RUN_WINDOW)).minus(DAY_OVERLAP);

        try (MailboxReader.Session session = mailbox.open()) {
            List<IncomingMail> mails = session.receivedSince(since);
            List<IncomingMail> replies = mails.stream()
                    .filter(mail -> !mail.automatic() && !AutoReplyDetector.isSystemSender(mail.from()))
                    .toList();

            Set<String> knownSenders = store.knownEmails(replies.stream().map(IncomingMail::from).collect(Collectors.toSet()));
            Map<String, Instant> firstReply = new HashMap<>();
            Map<String, Set<String>> originalRecipients = new HashMap<>();

            for (IncomingMail reply : replies) {
                if (knownSenders.contains(reply.from())) {
                    firstReply.merge(reply.from(), reply.receivedAt(), ReplySyncService::earlier);
                } else if (reply.inReplyTo() != null) {
                    // someone else at the company answered our mail: credit the person we wrote to
                    Set<String> wroteTo = originalRecipients.computeIfAbsent(reply.inReplyTo(),
                            id -> store.knownEmails(session.recipientsOf(id)));
                    wroteTo.forEach(email -> firstReply.merge(email, reply.receivedAt(), ReplySyncService::earlier));
                }
            }

            int marked = store.markResponded(firstReply, now);
            store.recordSuccess(now, mails.size(), marked);
            log.info("Reply sync done: since={}, scanned={}, automatic or system={}, replies matched={}, newly responded={}",
                    since, mails.size(), mails.size() - replies.size(), firstReply.size(), marked);
            return new Result(mails.size(), firstReply.size(), marked);
        } catch (RuntimeException e) {
            log.error("Reply sync failed: {}", e.getMessage(), e);
            store.recordFailure(now, e.getMessage());
            throw e;
        }
    }

    private static Instant earlier(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public record Result(int scanned, int matched, int newlyResponded) {
        static final Result SKIPPED = new Result(0, 0, 0);
    }
}
