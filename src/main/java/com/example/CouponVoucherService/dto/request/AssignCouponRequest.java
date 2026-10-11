package com.example.CouponVoucherService.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignCouponRequest {

    private Long couponId;
    private Long buyerId;

}
