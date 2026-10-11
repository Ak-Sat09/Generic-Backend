package com.example.CouponVoucherService.dto.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Shape accepted from the configured public coupon feed. */
@Getter
@Setter
@NoArgsConstructor
public class ExternalCoupon {
    private String id;
    private String name;
    private String description;
    private String couponCode;
    private LocalDateTime expiryDate;
    private Long sellerId;
}
