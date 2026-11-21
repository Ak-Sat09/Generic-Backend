package com.buytogether.security;

/**
 * The logged in user as seen by controllers, built from a verified JWT.
 */
public record AuthUser(String id, String phone) {
}
