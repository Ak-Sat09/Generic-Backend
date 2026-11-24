package com.example.emailoutreach;

import java.time.Instant;

/**
 * Header-level view of one received message. Body and attachments are never read.
 *
 * @param from       sender address, lower case
 * @param receivedAt when Gmail received it
 * @param inReplyTo  Message-ID of the mail this answers, or null
 * @param automatic  true for out-of-office and other auto-generated mail
 */
public record IncomingMail(String from, Instant receivedAt, String inReplyTo, boolean automatic) {
}
