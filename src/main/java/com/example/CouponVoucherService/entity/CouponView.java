package com.example.CouponVoucherService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.example.CouponVoucherService.config.MongoIdGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "coupon_views")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CouponView {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long couponId;

    private Long viewerId;

    private LocalDateTime viewedAt;
}
