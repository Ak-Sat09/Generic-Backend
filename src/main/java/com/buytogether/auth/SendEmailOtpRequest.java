package com.buytogether.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailOtpRequest(@NotBlank @Size(max = 254) String email) {
}
