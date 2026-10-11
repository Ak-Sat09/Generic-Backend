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
import lombok.Setter;

import java.time.LocalDateTime;

@Document(collection = "coupon_verification_votes")
@CompoundIndex(name = "uk_coupon_verification_coupon_user", def = "{'couponId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CouponVerificationVote {

    @Id
    @Builder.Default
    private Long id = MongoIdGenerator.next();

    private Long couponId;

    private Long userId;

    private Boolean worked;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
