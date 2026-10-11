package com.example.CouponVoucherService.dto.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CouponCreateEvent {
    private Long sellerId;
    private String name;
    private String description;
    private String couponCode;
    private LocalDateTime expiryDate;
}
