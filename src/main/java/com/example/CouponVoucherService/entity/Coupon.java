package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Coupon {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long sellerId;

    private String name;

    private String description;

    private String couponCode;

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /* ===== Domain Method ===== */

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }
}
