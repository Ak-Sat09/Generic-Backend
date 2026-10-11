package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CouponAssistantResponse {
    private String query;
    private String interpretedFilter;
    private List<AssistantCoupon> coupons;

    @Getter
    @Builder
    public static class AssistantCoupon {
        private Long id;
        private Long sellerId;
        private String name;
        private String description;
        private String couponCode;
        private LocalDateTime expiryDate;
        private int verificationScore;
        private long workedCount;
        private long notWorkedCount;
        private String reason;
    }
}
