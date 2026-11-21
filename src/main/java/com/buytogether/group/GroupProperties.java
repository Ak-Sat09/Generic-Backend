package com.buytogether.group;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "buy-together.groups")
public record GroupProperties(
        @Min(2) int minSize,
        @Min(2) int maxSize,
        @Min(1) int maxOpenPerUser,
        @Min(1) int maxDeadlineDays,
        @Min(1) int purgeAfterDays,
        @NotBlank String maintenanceCron
) {
}
