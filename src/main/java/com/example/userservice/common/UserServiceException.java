package com.example.userservice.common;

import org.springframework.http.HttpStatus;

/**
 * A failure the caller should see as-is: the status and a message that is safe to show to the user.
 */
public class UserServiceException extends RuntimeException {

    private final HttpStatus status;

    public UserServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static UserServiceException unauthorized(String message) {
        return new UserServiceException(HttpStatus.UNAUTHORIZED, message);
    }
}
