package com.buytogether.auth;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank String phone) {
}
