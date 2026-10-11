package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponVerificationResponse {
    private Long couponId;
    private long workedCount;
    private long notWorkedCount;
    private int verificationScore;
    private Boolean myVote;
}
