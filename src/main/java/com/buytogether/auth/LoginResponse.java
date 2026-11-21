package com.buytogether.auth;

import com.buytogether.user.UserResponse;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, boolean newUser, UserResponse user) {
}
