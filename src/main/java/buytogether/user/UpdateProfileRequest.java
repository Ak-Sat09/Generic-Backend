package com.buytogether.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 60) String city
) {
}
