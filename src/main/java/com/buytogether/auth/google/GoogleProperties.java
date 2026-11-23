package com.buytogether.auth.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth client from Google Cloud Console. The client id is public (it ships in the frontend);
 * Google login stays off while it is empty.
 */
@ConfigurationProperties(prefix = "buy-together.google")
public record GoogleProperties(String clientId) {
}
