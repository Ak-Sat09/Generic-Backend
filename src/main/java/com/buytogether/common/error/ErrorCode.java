package com.buytogether.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request has invalid fields"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request body could not be read"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Login required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to do this"),

    INVALID_PHONE(HttpStatus.BAD_REQUEST, "Enter a valid 10 digit Indian mobile number"),
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Please wait before asking for a new OTP"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP expired or was never sent, request a new one"),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "Wrong OTP"),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong attempts, request a new OTP"),
    OTP_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Could not send the OTP right now, try again"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),

    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Group not found"),
    GROUP_NOT_JOINABLE(HttpStatus.CONFLICT, "This group is not accepting members"),
    GROUP_FULL(HttpStatus.CONFLICT, "This group is already full"),
    ALREADY_MEMBER(HttpStatus.CONFLICT, "You are already in this group"),
    NOT_A_MEMBER(HttpStatus.FORBIDDEN, "Only group members can do this"),
    NOT_GROUP_CREATOR(HttpStatus.FORBIDDEN, "Only the person who created the group can do this"),
    CREATOR_CANNOT_LEAVE(HttpStatus.CONFLICT, "Group creator cannot leave, close the group instead"),
    GROUP_INACTIVE(HttpStatus.CONFLICT, "This group is no longer active"),
    OPEN_GROUP_EXISTS(HttpStatus.CONFLICT, "An open group for this brand already exists in your city, join it instead"),
    TOO_MANY_ACTIVE_GROUPS(HttpStatus.CONFLICT, "You already have the maximum number of open groups"),
    INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "Deadline is outside the allowed range"),
    INVALID_GROUP_SIZE(HttpStatus.BAD_REQUEST, "Group size is out of the allowed range"),

    ALREADY_REPORTED(HttpStatus.CONFLICT, "You have already reported this group"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
