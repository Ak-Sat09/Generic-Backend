package com.buytogether.group.cache;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "buy-together.cache")
public record CacheProperties(@Valid @NotNull Spec group, @Valid @NotNull Spec search) {

    public record Spec(@Min(1) long maxSize, @NotNull Duration ttl) {
    }
}
