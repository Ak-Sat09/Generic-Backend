package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SavedStateResponse {
    private Long couponId;
    private boolean saved;
}
