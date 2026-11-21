package com.buytogether.group;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateGroupRequest(
        @NotNull CarBrand brand,
        @NotBlank @Size(max = 60) String model,
        @NotBlank @Size(max = 60) String city,
        @NotNull Integer targetSize,
        @Size(max = 500) String note,
        @FutureOrPresent LocalDate deadline
) {
}
