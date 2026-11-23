package com.buytogether.auth.google;

import com.buytogether.common.email.EmailAddress;

/**
 * What we take from a verified Google ID token.
 */
public record GoogleIdentity(String subject, EmailAddress email, String name) {
}
