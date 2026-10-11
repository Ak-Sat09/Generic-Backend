package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCouponResponse {
    private Long id;
    private Long sellerId;
    private String name;
    private String description;
    private String couponCode;
    private LocalDateTime expiryDate;
    private boolean expired;
    private long viewCount;
    private long savedCount;
    private long requestCount;
    private long workedCount;
    private long notWorkedCount;
    private int verificationScore;
    private String riskLevel;
}
