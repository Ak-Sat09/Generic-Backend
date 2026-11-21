package com.buytogether.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank String phone,
        @NotBlank @Pattern(regexp = "\\d{4,8}", message = "OTP must be digits only") String otp,
        @Size(max = 60) String name
) {
}
