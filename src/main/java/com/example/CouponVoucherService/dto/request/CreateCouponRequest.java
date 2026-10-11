package com.example.CouponVoucherService.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateCouponRequest {

    private String name;
    private String description;
    private String couponCode;
    private LocalDateTime expiryDate;

}
