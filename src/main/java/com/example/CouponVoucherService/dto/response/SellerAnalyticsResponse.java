package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SellerAnalyticsResponse {
    private long couponCount;
    private long requestCount;
    private long acceptedRequestCount;
    private long chatCount;
    private long assignedCount;
    private long viewCount;
    private long workedCount;
    private long notWorkedCount;
    private List<CouponAnalytics> coupons;

    @Getter
    @Builder
    public static class CouponAnalytics {
        private Long couponId;
        private String couponName;
        private long viewCount;
        private long requestCount;
        private long acceptedRequestCount;
        private long chatCount;
        private long assignedCount;
        private long workedCount;
        private long notWorkedCount;
        private int verificationScore;
    }
}
