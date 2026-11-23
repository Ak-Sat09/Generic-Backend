package com.buytogether.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code credential} is the ID token the "Sign in with Google" button hands to the frontend.
 */
public record GoogleLoginRequest(@NotBlank String credential) {
}
