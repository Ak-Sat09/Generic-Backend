package com.example.emailoutreach;

import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.ReceivedDateTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Gmail over IMAP, read only and headers only. Folders are found by their special-use flag
 * (\All, \Trash), so it works whatever language the Gmail account uses ("Bin" or "Trash").
 */
@Component
class GmailMailboxReader implements MailboxReader {

    private static final Logger log = LoggerFactory.getLogger(GmailMailboxReader.class);
    private static final String[] HEADERS = {"In-Reply-To", "Auto-Submitted", "X-Autoreply", "X-Autorespond", "Precedence"};

    private final EmailOutreachProperties.Gmail gmail;

    GmailMailboxReader(EmailOutreachProperties properties) {
        this.gmail = properties.gmail();
    }

    @Override
    public MailboxReader.Session open() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", gmail.host());
        props.put("mail.imaps.port", String.valueOf(gmail.port()));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "60000");
        try {
            Store store = Session.getInstance(props).getStore("imaps");
            store.connect(gmail.host(), gmail.port(), gmail.username(), gmail.appPassword());
            return new GmailSession(store, gmail.username().trim().toLowerCase(Locale.ROOT));
        } catch (MessagingException e) {
            throw new MailboxException("Could not connect to Gmail", e);
        }
    }

    private static final class GmailSession implements MailboxReader.Session {

        private final Store store;
        private final String ownAddress;
        private final Folder allMail;
        private final Folder bin;

        GmailSession(Store store, String ownAddress) throws MessagingException {
            this.store = store;
            this.ownAddress = ownAddress;
            Folder foundAll = null;
            Folder foundBin = null;
            for (Folder folder : store.getDefaultFolder().list("*")) {
                if (folder instanceof IMAPFolder imap) {
                    for (String attribute : imap.getAttributes()) {
                        if ("\\All".equalsIgnoreCase(attribute)) {
                            foundAll = folder;
                        } else if ("\\Trash".equalsIgnoreCase(attribute)) {
                            foundBin = folder;
                        }
                    }
                }
            }
            if (foundAll == null) {
                throw new MailboxException("Gmail 'All Mail' folder not found. Is it hidden from IMAP in Gmail settings?", null);
            }
            this.allMail = foundAll;
            this.bin = foundBin;
            allMail.open(Folder.READ_ONLY);
            log.info("Reading Gmail folders '{}' and '{}'", allMail.getFullName(), bin == null ? "-" : bin.getFullName());
        }

        @Override
        public List<IncomingMail> receivedSince(Instant since) {
            List<IncomingMail> mails = new ArrayList<>(read(allMail, since));
            if (bin != null) {
                try {
                    bin.open(Folder.READ_ONLY);
                    mails.addAll(read(bin, since));
                } catch (MessagingException e) {
                    throw new MailboxException("Could not open the Bin folder", e);
                } finally {
                    closeQuietly(bin);
                }
            }
            return mails;
        }

        private List<IncomingMail> read(Folder folder, Instant since) {
            try {
                // IMAP SINCE works on whole days; the caller adds overlap and updates are idempotent
                Message[] found = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, Date.from(since)));
                FetchProfile profile = new FetchProfile();
                profile.add(FetchProfile.Item.ENVELOPE);
                for (String header : HEADERS) {
                    profile.add(header);
                }
                folder.fetch(found, profile);

                List<IncomingMail> mails = new ArrayList<>(found.length);
                for (Message message : found) {
                    String from = firstAddress(message.getFrom());
                    if (from == null || from.equals(ownAddress)) {
                        continue;
                    }
                    Date received = message.getReceivedDate();
                    mails.add(new IncomingMail(
                            from,
                            received == null ? Instant.now() : received.toInstant(),
                            firstMessageId(header(message, "In-Reply-To")),
                            AutoReplyDetector.isAutomatic(
                                    header(message, "Auto-Submitted"),
                                    header(message, "X-Autoreply") != null || header(message, "X-Autorespond") != null,
                                    header(message, "Precedence"),
                                    message.getSubject())));
                }
                return mails;
            } catch (MessagingException e) {
                throw new MailboxException("Could not read folder " + folder.getFullName(), e);
            }
        }

        @Override
        public Set<String> recipientsOf(String messageId) {
            try {
                Message[] found = allMail.search(new MessageIDTerm(messageId));
                Set<String> recipients = new LinkedHashSet<>();
                if (found.length > 0) {
                    addAll(recipients, found[0].getRecipients(Message.RecipientType.TO));
                    addAll(recipients, found[0].getRecipients(Message.RecipientType.CC));
                }
                recipients.remove(ownAddress);
                return recipients;
            } catch (MessagingException e) {
                log.warn("Could not look up original message {}: {}", messageId, e.getMessage());
                return Set.of();
            }
        }

        @Override
        public void close() {
            closeQuietly(allMail);
            try {
                if (store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                log.debug("Closing Gmail connection failed", e);
            }
        }

        private static void closeQuietly(Folder folder) {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
            } catch (MessagingException e) {
                log.debug("Closing folder failed", e);
            }
        }

        private static String header(Message message, String name) throws MessagingException {
            String[] values = message.getHeader(name);
            return values == null || values.length == 0 ? null : values[0];
        }

        private static String firstAddress(Address[] addresses) {
            if (addresses != null) {
                for (Address address : addresses) {
                    if (address instanceof InternetAddress internet && internet.getAddress() != null) {
                        return internet.getAddress().trim().toLowerCase(Locale.ROOT);
                    }
                }
            }
            return null;
        }

        private static void addAll(Set<String> into, Address[] addresses) {
            if (addresses != null) {
                for (Address address : addresses) {
                    if (address instanceof InternetAddress internet && internet.getAddress() != null) {
                        into.add(internet.getAddress().trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
    }

    /** In-Reply-To can list several ids; the first one is the message being answered. */
    static String firstMessageId(String inReplyTo) {
        if (inReplyTo == null) {
            return null;
        }
        int start = inReplyTo.indexOf('<');
        int end = inReplyTo.indexOf('>', start + 1);
        return start >= 0 && end > start ? inReplyTo.substring(start, end + 1) : null;
    }
}
