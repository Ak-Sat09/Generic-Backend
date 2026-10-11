package com.example.CouponVoucherService.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.coupon-feed")
public class CouponFeedProperties {
    private boolean enabled;
    private String url;
    private long fixedDelayMs = 300_000;
    private long initialDelayMs = 10_000;
    private int maxDedupeKeys = 10_000;
    private long defaultSellerId;
    private String priceyugeApiKey;
    private String priceyugeApiVersion = "v1";
    private String priceyugeStoreTag;
    private String priceyugeCategoryTag;
}
