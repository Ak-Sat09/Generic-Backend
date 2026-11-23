package com.buytogether.user;

import java.time.Instant;

public record UserResponse(String id, String phone, String email, String name, String city, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPhone(), user.getEmail(), user.getName(), user.getCity(),
                user.getCreatedAt());
    }
}
