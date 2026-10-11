package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SavedCouponResponse {
    private CouponResponse coupon;
    private LocalDateTime savedAt;
    private long daysUntilExpiry;
}
