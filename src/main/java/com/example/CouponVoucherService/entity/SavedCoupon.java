package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "saved_coupons")
@CompoundIndex(name = "uk_saved_coupon_buyer_coupon", def = "{'buyerId': 1, 'couponId': 1}", unique = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SavedCoupon {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long buyerId;

    private Long couponId;

    private LocalDateTime createdAt;
}
