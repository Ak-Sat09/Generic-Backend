package com.example.userservice.common;

/**
 * Response envelope used by every User Service endpoint, kept from the original API
 * so existing frontends keep working.
 */
public record ApiResponse(boolean success, String message, Object data) {

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null);
    }
}
