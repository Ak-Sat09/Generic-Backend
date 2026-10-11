package com.example.CouponVoucherService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CouponListResponse {

    private List<CouponResponse> coupons;

}
